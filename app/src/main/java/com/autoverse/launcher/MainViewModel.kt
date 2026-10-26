package com.autoverse.launcher

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoverse.launcher.core.connectivity.ConnectivityMonitor
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.diagnostics.DiagnosticEvent
import com.autoverse.launcher.core.diagnostics.DiagnosticLevel
import com.autoverse.launcher.core.diagnostics.DiagnosticLog
import com.autoverse.launcher.core.diagnostics.FrameSample
import com.autoverse.launcher.core.diagnostics.FrameSampleHistory
import com.autoverse.launcher.core.diagnostics.SystemSampler
import com.autoverse.launcher.core.diagnostics.SystemSnapshot
import com.autoverse.launcher.core.launcher.AppCatalog
import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.launcher.AppRepository
import com.autoverse.launcher.core.launcher.FavoritesRepository
import com.autoverse.launcher.core.launcher.LaunchOutcome
import com.autoverse.launcher.core.launcher.RecentAppsRepository
import com.autoverse.launcher.core.launcher.classifyLaunchFailure
import com.autoverse.launcher.core.phone.CallStateMonitor
import com.autoverse.launcher.core.phone.CallUiState
import com.autoverse.launcher.core.media.MediaCommand
import com.autoverse.launcher.core.media.MediaCommandOutcome
import com.autoverse.launcher.core.media.MediaSessionRepository
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.SystemMediaSessionRepository
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.location.SpeedometerManager
import com.autoverse.launcher.core.media.mediaCommandFailureMessage
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.obd.ObdAdapter
import com.autoverse.launcher.core.obd.ObdConnectionState
import com.autoverse.launcher.core.obd.ObdDtcState
import com.autoverse.launcher.core.obd.SystemObdRepository
import com.autoverse.launcher.core.sound.ThemeSoundManager
import com.autoverse.launcher.core.voice.OfflineVoiceEngine
import com.autoverse.launcher.core.voice.OfflineVoiceState
import com.autoverse.launcher.core.weather.WeatherRepository
import com.autoverse.launcher.core.weather.WeatherUiState
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.MapsForegroundVerdict
import com.autoverse.launcher.core.navigation.MapsStatus
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.mapsStatusOf
import com.autoverse.launcher.core.navigation.providerForPackage
import com.autoverse.launcher.core.navigation.NavigationPlacesRepository
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.navigation.NavigationRepository
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.navigation.SavedPlace
import com.autoverse.launcher.core.navigation.SystemNavigationRepository
import com.autoverse.launcher.core.navigation.navigationFailureMessage
import com.autoverse.launcher.core.navigation.resolveNavigationProvider
import com.autoverse.launcher.core.performance.DeviceProfile
import com.autoverse.launcher.core.performance.FrameTimeTracker
import com.autoverse.launcher.core.performance.PerformanceSpec
import com.autoverse.launcher.core.performance.performanceSpecFor
import com.autoverse.launcher.core.preferences.SettingsRepository
import com.autoverse.launcher.core.util.millisUntilNextMinute
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val appRepository = AppRepository(application)
    private val favoritesRepository = FavoritesRepository(application)
    private val recentAppsRepository = RecentAppsRepository(application)
    private val navigationPlacesRepository = NavigationPlacesRepository(application)
    private val mediaRepository: MediaSessionRepository = SystemMediaSessionRepository(application)
    private val navigationRepository: NavigationRepository = SystemNavigationRepository(application)
    private val obdRepository = SystemObdRepository(application)
    private val callStateMonitor = CallStateMonitor(application)
    private val connectivityMonitor = ConnectivityMonitor(application)
    private val systemSampler = SystemSampler(application)

    /**
     * Phase 17 — bounded in-memory record of explicit, user-meaningful events
     * for the diagnostics log panel. Nothing here is written to disk, sent
     * anywhere, or recorded unless a call site passes a message.
     */
    private val diagnosticLog = DiagnosticLog()

    /** Recorded diagnostic events, newest first. */
    val diagnosticsEvents: StateFlow<List<DiagnosticEvent>> = diagnosticLog.entries

    private val mediaRefreshTrigger = MutableStateFlow(0)

    /**
     * True while the platform-wide "remove animations" setting is active
     * (animator duration scale of zero — Android's accessibility control for
     * motion). Watched live via a content observer so motion stops the moment
     * the setting changes; the UI folds it into [effectiveReducedMotion][com.autoverse.launcher.core.design.tokens.effectiveReducedMotion].
     */
    private val _systemAnimationsDisabled = MutableStateFlow(isSystemAnimationsDisabled(application))
    val systemAnimationsDisabled: StateFlow<Boolean> = _systemAnimationsDisabled.asStateFlow()

    private val animationScaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val disabled = isSystemAnimationsDisabled(getApplication())
            if (disabled != _systemAnimationsDisabled.value) {
                diagnosticLog.record(
                    if (disabled) DiagnosticLevel.WARN else DiagnosticLevel.INFO,
                    if (disabled) {
                        "System animations disabled — motion is reduced"
                    } else {
                        "System animations re-enabled"
                    },
                )
            }
            _systemAnimationsDisabled.value = disabled
        }
    }

    /**
     * Media surface state. Re-observes from scratch on every
     * [refreshMediaSession] (resume, retry); collection stops shortly after
     * the UI goes away, releasing the session callbacks.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaState: StateFlow<MediaUiState> = mediaRefreshTrigger
        .flatMapLatest { mediaRepository.observe() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), MediaUiState.Connecting)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /**
     * Wi-Fi / Bluetooth indicator state for the dashboard header. Listeners
     * register only while the dashboard collects and tear down shortly
     * after it goes away; unverifiable links stay UNKNOWN and hidden.
     */
    val connectivity: StateFlow<ConnectivityStatus> = connectivityMonitor.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), ConnectivityStatus())

    /**
     * The single rendering-behavior contract for the current settings
     * (profile + "Prefer smoothness" override). Eager so the clock tick rate
     * and artwork budgets below never race the first frame.
     */
    val performanceSpec: StateFlow<PerformanceSpec> = settings
        .map { settings -> performanceSpecFor(settings.profile, settings.preferSmoothness) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, PerformanceSpec.Default)

    private val _navigationProviders = MutableStateFlow<List<NavigationProvider>>(emptyList())

    /** Navigation-capable targets discovered from the package manager. */
    val navigationProviders: StateFlow<List<NavigationProvider>> = _navigationProviders.asStateFlow()

    /**
     * The handoff target the UI should use. A stored preference whose app is
     * not installed resolves to the system default, flagged so the UI can
     * explain the fallback instead of silently ignoring the choice.
     */
    val navigationProvider: StateFlow<ResolvedNavigationProvider> =
        combine(settings, _navigationProviders) { currentSettings, providers ->
            resolveNavigationProvider(currentSettings.navigationPackage, providers)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = resolveNavigationProvider(AppSettings().navigationPackage, emptyList()),
        )

    /** Whether Google Maps is installed, as of the last provider discovery. */
    val mapsStatus: StateFlow<MapsStatus> = _navigationProviders
        .map(::mapsStatusOf)
        .stateIn(viewModelScope, SharingStarted.Eagerly, MapsStatus.UNKNOWN)

    /**
     * Dashboard clock. Balanced/Enhanced tick every second; the Constrained
     * workload re-reads the wall clock only when the displayed minute
     * changes, removing 59 of 60 clock recompositions per minute.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val now: StateFlow<LocalDateTime> = performanceSpec
        .map { spec -> spec.minuteAlignedClock }
        .distinctUntilChanged()
        .flatMapLatest { minuteAligned ->
            flow {
                while (true) {
                    val current = LocalDateTime.now()
                    emit(current)
                    delay(if (minuteAligned) millisUntilNextMinute(current) else 1_000L)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), LocalDateTime.now())

    val deviceProfile: DeviceProfile = DeviceProfile.sample(application)

    val frameTracker = FrameTimeTracker()
    val frameSnapshot: StateFlow<FrameTimeTracker.Snapshot> = frameTracker.snapshot

    /** True while the frame tracker is actively sampling; drives diagnostics copy. */
    val frameTrackingActive: StateFlow<Boolean> = frameTracker.running

    private val frameSampleHistory = FrameSampleHistory()

    /** Bounded one-second FPS windows, oldest first, for the history chart. */
    val frameHistory: StateFlow<List<FrameSample>> = frameSampleHistory.samples

    private val _appListState = MutableStateFlow<AppListState>(AppListState.Loading)
    val appListState: StateFlow<AppListState> = _appListState.asStateFlow()

    private var refreshJob: Job? = null
    private var diagnosticsRefreshJob: Job? = null
    private var diagnosticsSamplingJob: Job? = null

    /** null until the startup read resolves; false means the file was unreadable. */
    private val _settingsStoreReadable = MutableStateFlow<Boolean?>(null)
    val settingsStoreReadable: StateFlow<Boolean?> = _settingsStoreReadable.asStateFlow()

    private val _diagnosticsRefreshing = MutableStateFlow(false)

    /** True while a manual Refresh All pass is in flight. */
    val diagnosticsRefreshing: StateFlow<Boolean> = _diagnosticsRefreshing.asStateFlow()

    private val _systemSnapshot = MutableStateFlow<SystemSnapshot?>(null)

    /** Latest resource sample; keeps its last reading while sampling is off. */
    val systemSnapshot: StateFlow<SystemSnapshot?> = _systemSnapshot.asStateFlow()

    private var lastNotificationAccess: Boolean? = null

    val favorites: StateFlow<List<String>> = favoritesRepository.favorites
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteApps: StateFlow<List<InstalledApp>> =
        combine(_appListState, favorites) { listState, favoriteKeys ->
            if (listState is AppListState.Ready) {
                AppCatalog.resolveFavorites(favoriteKeys, listState.apps)
            } else {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Apps launched through AutoVerse, most recent first. Resolution waits
     * for the app list so keys map to real, installed applications; stale
     * keys (uninstalled apps) simply do not resolve.
     */
    val recentApps: StateFlow<List<InstalledApp>> =
        combine(_appListState, recentAppsRepository.recentKeys) { listState, keys ->
            if (listState is AppListState.Ready) {
                val byKey = listState.apps.associateBy { it.key }
                keys.mapNotNull { byKey[it] }
            } else {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- Incoming-call panel (Home side region) ----
    val callState: StateFlow<CallUiState> = callStateMonitor.callState

    val hasCallPermission: Boolean
        get() = callStateMonitor.hasReadPhoneStatePermission

    /** Starts callback-driven call observation; only while Home is visible. */
    fun startCallMonitoring() = callStateMonitor.start()

    fun stopCallMonitoring() = callStateMonitor.stop()

    /** Phase 18 — locally saved places, newest first. Never leaves the device. */
    val savedPlaces: StateFlow<List<SavedPlace>> = navigationPlacesRepository.savedPlaces
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Phase 18 — destinations recorded only after a successful handoff. */
    val recentDestinations: StateFlow<List<RecentDestination>> =
        navigationPlacesRepository.recentDestinations
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // ---- GPS Speedometer & Trip Tracker ----
    private val speedometerManager = SpeedometerManager(application)
    val speedometerData: StateFlow<SpeedometerData> = speedometerManager.speedometerData

    fun startSpeedometer() = speedometerManager.start()
    fun stopSpeedometer() = speedometerManager.stop()
    fun resetTrip() = speedometerManager.resetTrip()

    // ---- Weather ----
    private val weatherRepository = WeatherRepository()
    val weatherState: StateFlow<WeatherUiState> = weatherRepository.weatherState

    fun refreshWeather(force: Boolean = false) {
        val loc = speedometerData.value
        viewModelScope.launch {
            weatherRepository.updateWeather(loc.latitude, loc.longitude, force)
        }
    }

    // ---- Live vehicle data (OBD over Bluetooth ELM327) ----

    private val _obdState = MutableStateFlow<ObdConnectionState>(ObdConnectionState.Disconnected)
    val obdState: StateFlow<ObdConnectionState> = _obdState.asStateFlow()

    private val _obdPairedAdapters = MutableStateFlow<List<ObdAdapter>>(emptyList())
    val obdPairedAdapters: StateFlow<List<ObdAdapter>> = _obdPairedAdapters.asStateFlow()

    private val _obdDtcState = MutableStateFlow<ObdDtcState>(ObdDtcState.Idle)
    val obdDtcState: StateFlow<ObdDtcState> = _obdDtcState.asStateFlow()

    private var pendingDtcRequest = false
    private var obdJob: Job? = null
    private var obdStop = false

    fun refreshObdAdapters() {
        _obdPairedAdapters.value = obdRepository.listPairedAdapters()
    }

    fun requestDtcs() {
        if (_obdState.value is ObdConnectionState.Connected) {
            _obdDtcState.value = ObdDtcState.Reading
            pendingDtcRequest = true
        } else {
            _obdDtcState.value = ObdDtcState.Unavailable("Connect to OBD adapter first")
        }
    }

    /**
     * Starts a polling session against the specified or first bonded ELM327 adapter.
     * Requires BLUETOOTH_CONNECT on API 31+; surfaces the permission case
     * instead of crashing. Never fabricates readings — see ObdProtocol.
     */
    fun connectObd(deviceAddress: String? = null) {
        if (obdJob?.isActive == true) return
        if (!obdRepository.hasConnectPermission) {
            _obdState.value = ObdConnectionState.PermissionRequired
            return
        }
        if (!obdRepository.isBluetoothReady()) {
            _obdState.value = ObdConnectionState.Error("Bluetooth is off — turn it on and try again")
            return
        }
        obdStop = false
        var deviceName = if (deviceAddress != null) {
            _obdPairedAdapters.value.firstOrNull { it.address == deviceAddress }?.name ?: "OBD adapter"
        } else {
            obdRepository.findPairedAdapter() ?: "OBD adapter"
        }
        _obdState.value = ObdConnectionState.Connecting(deviceName)
        _obdDtcState.value = ObdDtcState.Idle
        pendingDtcRequest = false

        obdJob = viewModelScope.launch {
            try {
                obdRepository.runSession(
                    onState = { state ->
                        when (state) {
                            is ObdConnectionState.Connecting -> deviceName = state.deviceName
                            else -> Unit
                        }
                        _obdState.value = state
                    },
                    onSample = { sample ->
                        _obdState.value = ObdConnectionState.Connected(deviceName, sample)
                    },
                    shouldStop = { obdStop },
                    deviceAddress = deviceAddress,
                    consumeDtcRequest = {
                        val req = pendingDtcRequest
                        pendingDtcRequest = false
                        req
                    },
                    onDtcs = { dtcs ->
                        _obdDtcState.value = if (dtcs != null) {
                            ObdDtcState.Codes(dtcs)
                        } else {
                            ObdDtcState.Unavailable("ECU did not return diagnostic codes")
                        }
                    },
                )
                _obdState.value = ObdConnectionState.Disconnected
            } catch (cancellation: CancellationException) {
                _obdState.value = ObdConnectionState.Disconnected
            } catch (failure: Exception) {
                _obdState.value = ObdConnectionState.Error(failure.message ?: "OBD connection failed")
            }
        }
    }

    fun disconnectObd() {
        obdStop = true
        obdRepository.disconnect()
        obdJob?.cancel()
        obdJob = null
        _obdState.value = ObdConnectionState.Disconnected
    }

    // ---- Tier 2 part 2: offline wake-word voice (Vosk, opt-in) ----

    private val _offlineVoiceState = MutableStateFlow<OfflineVoiceState>(OfflineVoiceState.Disabled)

    /** Download/listen/capture states surfaced to the cockpit orb. */
    val offlineVoiceState: StateFlow<OfflineVoiceState> = _offlineVoiceState.asStateFlow()

    private val _offlineVoiceCommands = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Transcribed commands; the cockpit routes them like tap-to-talk. */
    val offlineVoiceCommands: SharedFlow<String> = _offlineVoiceCommands.asSharedFlow()

    private val offlineVoiceEngine: OfflineVoiceEngine by lazy {
        OfflineVoiceEngine(
            context = application,
            scope = viewModelScope,
            onState = { _offlineVoiceState.value = it },
            onCommand = { command -> _offlineVoiceCommands.tryEmit(command) },
        )
    }

    private var appForeground = false

    /** Mic-permission prompt result is pushed through the bridge handler. */
    fun onMicPermissionResult(granted: Boolean) {
        if (granted) {
            applyOfflineVoice(enabled = settings.value.offlineVoiceEnabled, micGranted = true)
        } else {
            _offlineVoiceState.value = OfflineVoiceState.Error(OfflineVoiceEngine.MIC_PERMISSION_MESSAGE)
        }
    }

    fun hasMicPermission(): Boolean = offlineVoiceEngine.hasMicPermission

    fun onAppForeground() {
        appForeground = true
        speedometerManager.start()
        refreshObdAdapters()
        applyOfflineVoice(enabled = settings.value.offlineVoiceEnabled, micGranted = null)
    }

    fun onAppBackground() {
        appForeground = false
        speedometerManager.stop()
        if (settings.value.offlineVoiceEnabled) {
            offlineVoiceEngine.stop()
            _offlineVoiceState.value = OfflineVoiceState.Disabled
        }
    }

    private fun applyOfflineVoice(enabled: Boolean, micGranted: Boolean?) {
        when {
            !enabled -> {
                offlineVoiceEngine.stop()
                _offlineVoiceState.value = OfflineVoiceState.Disabled
            }
            micGranted == false ->
                _offlineVoiceState.value = OfflineVoiceState.Error(OfflineVoiceEngine.MIC_PERMISSION_MESSAGE)
            !offlineVoiceEngine.hasMicPermission ->
                _offlineVoiceState.value = OfflineVoiceState.Error(OfflineVoiceEngine.MIC_PERMISSION_MESSAGE)
            !appForeground -> offlineVoiceEngine.stop() // battery: never listen in background
            else -> offlineVoiceEngine.start()
        }
    }

    fun setOfflineVoiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOfflineVoiceEnabled(enabled)
            // the settings collector applies the runtime change
        }
    }

    fun setPremiumUnlocked(unlocked: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPremiumUnlocked(unlocked)
        }
    }

    fun setOnboardingComplete(complete: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOnboardingComplete(complete)
        }
    }

    fun setTheme(themeId: AutoVerseThemeId) {
        viewModelScope.launch {
            settingsRepository.setTheme(themeId)
            diagnosticLog.record(DiagnosticLevel.INFO, "Theme changed to ${themeId.displayName}")
        }
        if (settings.value.themeSoundsEnabled) {
            ThemeSoundManager.playConfirmation()
        }
    }

    fun setThemeSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThemeSoundsEnabled(enabled)
        }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        viewModelScope.launch {
            settingsRepository.setAppearanceMode(mode)
            diagnosticLog.record(DiagnosticLevel.INFO, "Appearance changed to ${mode.displayName}")
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReducedMotion(enabled)
            diagnosticLog.record(
                DiagnosticLevel.INFO,
                if (enabled) "Reduced motion enabled" else "Reduced motion disabled",
            )
        }
    }

    fun setProfile(profile: PerformanceProfile) {
        viewModelScope.launch {
            settingsRepository.setProfile(profile)
            diagnosticLog.record(
                DiagnosticLevel.INFO,
                "Performance profile changed to ${profile.displayName}",
            )
        }
    }

    fun setPreferSmoothness(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPreferSmoothness(enabled)
            diagnosticLog.record(
                DiagnosticLevel.INFO,
                if (enabled) "Prefer smoothness enabled" else "Prefer smoothness disabled",
            )
        }
    }

    fun setClock24h(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setClock24h(enabled) }
    }

    /** Persists the user text scale; the repository clamps to the safe range. */
    fun setTextScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setTextScale(scale) }
    }

    /** Apps page: collapse or restore the Recently Used panel (persisted). */
    fun setAppsRecentCollapsed(collapsed: Boolean) {
        viewModelScope.launch { settingsRepository.setAppsRecentCollapsed(collapsed) }
    }

    /** Maps page: collapse or restore the destinations panel (persisted). */
    fun setMapsPlacesCollapsed(collapsed: Boolean) {
        viewModelScope.launch { settingsRepository.setMapsPlacesCollapsed(collapsed) }
    }

    /** Forgets AutoVerse's own app-launch history. */
    fun clearRecentApps() {
        viewModelScope.launch {
            recentAppsRepository.clear()
            diagnosticLog.record(DiagnosticLevel.INFO, "Recently used apps cleared")
        }
    }

    /** Forgets recent destinations; saved places stay. */
    fun clearRecentDestinations() {
        viewModelScope.launch {
            navigationPlacesRepository.clearRecents()
            diagnosticLog.record(DiagnosticLevel.INFO, "Recent destinations cleared")
        }
    }

    fun setNavigationPackage(packageName: String?) {
        viewModelScope.launch {
            settingsRepository.setNavigationPackage(packageName)
            diagnosticLog.record(
                DiagnosticLevel.INFO,
                if (packageName == null) {
                    "Navigation app preference cleared"
                } else {
                    "Navigation app preference changed"
                },
            )
        }
    }

    /**
     * Restores settings to factory defaults. Favorites live in a separate
     * store and are user content, so a settings reset keeps them.
     */
    fun resetSettings() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            diagnosticLog.record(DiagnosticLevel.WARN, "Settings reset to defaults")
        }
    }

    /** First-load trigger; safe to call on every composition start. */
    fun ensureAppsLoaded() {
        if (_appListState.value is AppListState.Loading) refreshApps()
    }

    private var lastAppScanElapsedMs = 0L

    /**
     * Resume-time refresh: package broadcasts only reach AutoVerse while it is
     * alive, so an app installed while another app was in front is picked up
     * here. Bounded to one scan per minute so resumes never rescan in a loop.
     */
    fun refreshAppsIfStale() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastAppScanElapsedMs >= APP_RESCAN_INTERVAL_MS && refreshJob?.isActive != true) {
            refreshApps()
        }
    }

    /**
     * Re-queries installed apps off the main thread. A previously loaded list
     * stays visible while refreshing (and if the refresh fails) so package
     * updates never blank the drawer; stale favorites are pruned on success.
     */
    fun refreshApps() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { loadApps() }
    }

    /**
     * The actual scan; returns true on success. Failures keep the previous
     * [AppListState.Ready] list visible and are recorded for diagnostics
     * without user content; the drawer shows the specific error instead.
     */
    private suspend fun loadApps(): Boolean {
        if (_appListState.value !is AppListState.Ready) {
            _appListState.value = AppListState.Loading
        }
        val apps = try {
            withContext(Dispatchers.IO) { appRepository.loadInstalledApps() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            diagnosticLog.record(DiagnosticLevel.ERROR, "App list scan failed")
            if (_appListState.value !is AppListState.Ready) {
                _appListState.value =
                    AppListState.Error(failure.message ?: "App list unavailable")
            }
            return false
        }
        _appListState.value = AppListState.Ready(apps)
        lastAppScanElapsedMs = android.os.SystemClock.elapsedRealtime()
        diagnosticLog.record(DiagnosticLevel.INFO, "App list loaded — ${apps.size} apps")
        favoritesRepository.pruneStale(apps.asSequence().map { it.key }.toHashSet())
        _navigationProviders.value = try {
            navigationRepository.availableProviders()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            _navigationProviders.value
        }
        return true
    }

    fun toggleFavorite(appKey: String) {
        viewModelScope.launch { favoritesRepository.toggle(appKey) }
    }

    /**
     * Replaces the whole favorites list in launch order. Used by the drawer's
     * edit mode, which stages changes locally and persists them on Save.
     */
    fun setFavorites(order: List<String>) {
        viewModelScope.launch { favoritesRepository.setFavorites(order) }
    }

    /**
     * Phase 18 — persists a saved place under [label]; duplicates by
     * destination text are replaced, not stacked. Saved entries are
     * user-created only and stay local.
     */
    suspend fun saveSavedPlace(label: String, destination: NavigationDestination): Boolean = try {
        navigationPlacesRepository.savePlace(label, destination)
        diagnosticLog.record(DiagnosticLevel.INFO, "Saved place added")
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        diagnosticLog.record(DiagnosticLevel.WARN, "A place could not be saved")
        false
    }

    /** Phase 18 — removes [place] from the saved list. */
    fun removeSavedPlace(place: SavedPlace) {
        viewModelScope.launch {
            navigationPlacesRepository.removePlace(place)
            diagnosticLog.record(DiagnosticLevel.INFO, "Saved place removed")
        }
    }

    /**
     * Launches the app's launcher activity with the platform-standard MAIN/
     * LAUNCHER intent. Failures are classified instead of thrown; a missing
     * app also drops its favorite entry and refreshes the list. Failure
     * events are recorded without app labels (the UI explains specifics).
     */
    suspend fun launchApp(app: InstalledApp): LaunchOutcome {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val outcome = try {
            getApplication<Application>().startActivity(intent)
            LaunchOutcome.LAUNCHED
        } catch (failure: Throwable) {
            classifyLaunchFailure(failure)
        }
        if (outcome == LaunchOutcome.LAUNCHED) {
            // Bounded local history of AutoVerse-initiated launches only.
            recentAppsRepository.record(app.key)
        } else if (outcome == LaunchOutcome.NOT_FOUND) {
            diagnosticLog.record(
                DiagnosticLevel.WARN,
                "An app was missing; its favorite entry was removed",
            )
            favoritesRepository.remove(app.key)
            refreshApps()
        } else if (outcome != LaunchOutcome.LAUNCHED) {
            diagnosticLog.record(DiagnosticLevel.WARN, "An app could not be opened")
        }
        return outcome
    }

    suspend fun loadAppIcon(app: InstalledApp): ImageBitmap? = appRepository.loadIcon(app)

    /** The app typed destinations go to, resolved from the stored preference at tap time. */
    val destinationTarget: NavigationProvider
        get() = providerForPackage(settings.value.navigationPackage)

    /**
     * Hands [destination] to the preferred navigation app (Google Maps unless
     * the user chose another). The provider comes from the stored preference
     * at tap time — never from the discovery list, which may still be empty —
     * and the repository re-checks the real install state before launching.
     * A destination is recorded as recent only when Android accepted the request.
     */
    suspend fun navigateToDestination(destination: NavigationDestination): NavigationOutcome {
        val provider = destinationTarget
        val outcome = navigationRepository.launch(destination, provider)
        recordLaunchOutcome(provider.label, outcome)
        if (outcome == NavigationOutcome.REQUESTED) {
            navigationPlacesRepository.recordRecent(destination)
        }
        return outcome
    }

    /** "Open Google Maps" always means Google Maps, whatever the destination preference is. */
    suspend fun openGoogleMaps(): NavigationOutcome {
        val outcome = navigationRepository.launch(null, NavigationProvider.GoogleMaps)
        recordLaunchOutcome(NavigationProvider.GoogleMaps.label, outcome)
        return outcome
    }

    private fun recordLaunchOutcome(label: String, outcome: NavigationOutcome) {
        if (outcome == NavigationOutcome.REQUESTED) {
            diagnosticLog.record(DiagnosticLevel.INFO, "$label launch requested")
        } else {
            diagnosticLog.record(DiagnosticLevel.WARN, navigationFailureMessage(label, outcome))
        }
    }

    /** What the lifecycle showed after a requested launch; recorded for diagnostics. */
    fun recordLaunchVerdict(label: String, verdict: MapsForegroundVerdict) {
        val (level, message) = when (verdict) {
            MapsForegroundVerdict.LEFT_AUTOVERSE ->
                DiagnosticLevel.INFO to "$label came to the front (AutoVerse paused)"
            MapsForegroundVerdict.STAYED_ON_AUTOVERSE ->
                DiagnosticLevel.WARN to "$label launch was accepted but AutoVerse stayed in front"
            MapsForegroundVerdict.UNKNOWN_MULTI_WINDOW ->
                DiagnosticLevel.INFO to "$label launch requested in split-screen; visibility not confirmed"
        }
        diagnosticLog.record(level, message)
    }

    /** Re-reads session access and state; safe to call on every resume. */
    fun refreshMediaSession() {
        mediaRefreshTrigger.value += 1
    }

    /**
     * Manual "Refresh All" on the diagnostics screen: re-scans installed
     * apps, re-reads media session state, and takes a fresh resource sample.
     * Deliberately narrow — it refreshes safe diagnostic state only and never
     * toggles settings, launches anything, or changes access.
     */
    fun refreshDiagnostics() {
        diagnosticsRefreshJob?.cancel()
        diagnosticsRefreshJob = viewModelScope.launch {
            _diagnosticsRefreshing.value = true
            diagnosticLog.record(DiagnosticLevel.INFO, "Manual refresh requested")
            val appsLoaded = loadApps()
            refreshMediaSession()
            withContext(Dispatchers.IO) { _systemSnapshot.value = systemSampler.sample() }
            _diagnosticsRefreshing.value = false
            if (appsLoaded) {
                diagnosticLog.record(DiagnosticLevel.INFO, "Refresh complete")
            } else {
                diagnosticLog.record(DiagnosticLevel.WARN, "Refresh finished with errors")
            }
        }
    }

    /**
     * Runs or stops the periodic resource sampler for the diagnostics screen.
     * Sampling only happens while the screen is visible and resumed (driven
     * by AppRoot); the last reading survives so the panel still shows it
     * while paused.
     */
    fun setDiagnosticsSampling(enabled: Boolean) {
        diagnosticsSamplingJob?.cancel()
        diagnosticsSamplingJob = null
        if (!enabled) return
        diagnosticsSamplingJob = viewModelScope.launch {
            while (true) {
                withContext(Dispatchers.IO) { _systemSnapshot.value = systemSampler.sample() }
                delay(SYSTEM_SAMPLE_INTERVAL_MS)
            }
        }
    }

    /**
     * Reports the notification-access state observed on resume. Records only
     * changes (granted / revoked); the first observation is state, not an
     * event, and stays out of the log.
     */
    fun noteNotificationAccess(granted: Boolean) {
        val previous = lastNotificationAccess
        lastNotificationAccess = granted
        when {
            previous == true && !granted -> diagnosticLog.record(
                DiagnosticLevel.WARN,
                "Notification access revoked",
            )

            previous == false && granted -> diagnosticLog.record(
                DiagnosticLevel.INFO,
                "Notification access granted",
            )
        }
    }

    /**
     * Clears the diagnostics log ("Clear log" on the detail view). The clear
     * itself is not recorded — an empty log means exactly that.
     */
    fun clearDiagnosticsLog() {
        diagnosticLog.clear()
    }

    /**
     * Sends a transport command; any non-SENT outcome triggers a state
     * refresh so the card reflects reality (access revoked, session gone).
     * Failures are recorded with the same wording the media card shows.
     */
    suspend fun sendMediaCommand(command: MediaCommand): MediaCommandOutcome {
        val outcome = mediaRepository.send(command)
        if (outcome != MediaCommandOutcome.SENT) {
            diagnosticLog.record(
                if (outcome == MediaCommandOutcome.ACCESS_LOST) {
                    DiagnosticLevel.ERROR
                } else {
                    DiagnosticLevel.WARN
                },
                mediaCommandFailureMessage(outcome),
            )
            refreshMediaSession()
        }
        return outcome
    }

    /**
     * Asks the featured player to jump to a queue item. Failure handling
     * mirrors [sendMediaCommand]; NOT_SUPPORTED/FAILED surface the same
     * snackbar copy so unsupported players degrade gracefully.
     */
    suspend fun skipMediaToQueue(queueId: Long): MediaCommandOutcome {
        val outcome = mediaRepository.skipToQueueItem(queueId)
        if (outcome != MediaCommandOutcome.SENT) {
            diagnosticLog.record(
                if (outcome == MediaCommandOutcome.ACCESS_LOST) {
                    DiagnosticLevel.ERROR
                } else {
                    DiagnosticLevel.WARN
                },
                mediaCommandFailureMessage(outcome),
            )
            refreshMediaSession()
        }
        return outcome
    }

    /**
     * Seeks the featured session to an absolute position (cockpit scrubber).
     * Failure handling mirrors [sendMediaCommand] so a lost session or
     * revoked access is reflected immediately.
     */
    suspend fun seekMediaTo(positionMs: Long): MediaCommandOutcome {
        val outcome = mediaRepository.seekTo(positionMs)
        if (outcome != MediaCommandOutcome.SENT) {
            diagnosticLog.record(
                if (outcome == MediaCommandOutcome.ACCESS_LOST) {
                    DiagnosticLevel.ERROR
                } else {
                    DiagnosticLevel.WARN
                },
                mediaCommandFailureMessage(outcome),
            )
            refreshMediaSession()
        }
        return outcome
    }

    /** Embedded artwork first, then the session-provided URI. */
    suspend fun loadMediaArtwork(state: MediaUiState): ImageBitmap? {
        val active = state as? MediaUiState.Active ?: return null
        active.embeddedArtwork?.let { return it }
        val uri = active.artworkUri ?: return null
        return mediaRepository.loadArtwork(uri, performanceSpec.value.artworkTargetPx)
    }

    private val memoryTrimCallback: ComponentCallbacks2 = object : ComponentCallbacks2 {
        override fun onTrimMemory(level: Int) {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
                trimImageCaches()
            }
        }

        override fun onLowMemory() {
            trimImageCaches()
        }

        override fun onConfigurationChanged(newConfig: Configuration) = Unit
    }

    init {
        viewModelScope.launch {
            settings.map { it.offlineVoiceEnabled }.distinctUntilChanged().collect { enabled ->
                applyOfflineVoice(enabled = enabled, micGranted = null)
            }
        }

        application.registerComponentCallbacks(memoryTrimCallback)
        // One-shot schema upgrade; a no-op while the stamp is current. The
        // result also feeds the diagnostics store-health tile and startup log.
        viewModelScope.launch {
            val readable = settingsRepository.migrateIfNeeded()
            _settingsStoreReadable.value = readable
            if (readable) {
                diagnosticLog.record(DiagnosticLevel.INFO, "Settings loaded")
            } else {
                diagnosticLog.record(
                    DiagnosticLevel.WARN,
                    "Settings file unreadable — defaults are in use",
                )
            }
        }
        application.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            animationScaleObserver,
        )
        // Bridges published FPS windows into the bounded history. A session
        // change means the tracker restarted, so stale samples are dropped.
        viewModelScope.launch {
            var lastSessionId = -1L
            frameTracker.snapshot.collect { snapshot ->
                if (snapshot.sessionId != lastSessionId) {
                    lastSessionId = snapshot.sessionId
                    frameSampleHistory.clear()
                }
                if (snapshot.atMillis > 0L) {
                    frameSampleHistory.add(snapshot.fps, snapshot.atMillis)
                }
            }
        }
        diagnosticLog.record(
            DiagnosticLevel.INFO,
            "Launcher started (v${BuildConfig.VERSION_NAME})",
        )
    }

    override fun onCleared() {
        speedometerManager.stop()
        obdStop = true
        obdRepository.disconnect()
        getApplication<Application>().unregisterComponentCallbacks(memoryTrimCallback)
        getApplication<Application>().contentResolver.unregisterContentObserver(animationScaleObserver)
    }

    /** Drops decoded-bitmap caches; every entry rebuilds lazily on next use. */
    private fun trimImageCaches() {
        appRepository.trimMemory()
        mediaRepository.trimMemory()
    }

    private companion object {
        const val SYSTEM_SAMPLE_INTERVAL_MS = 1_000L
        const val APP_RESCAN_INTERVAL_MS = 60_000L
    }
}

/** Reads the platform-wide animator duration scale; zero means "removed". */
private fun isSystemAnimationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
