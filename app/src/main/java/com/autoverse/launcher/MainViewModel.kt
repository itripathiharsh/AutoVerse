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
import com.autoverse.launcher.core.launcher.classifyLaunchFailure
import com.autoverse.launcher.core.media.MediaCommand
import com.autoverse.launcher.core.media.MediaCommandOutcome
import com.autoverse.launcher.core.media.MediaSessionRepository
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.SystemMediaSessionRepository
import com.autoverse.launcher.core.media.mediaCommandFailureMessage
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
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
    private val navigationPlacesRepository = NavigationPlacesRepository(application)
    private val mediaRepository: MediaSessionRepository = SystemMediaSessionRepository(application)
    private val navigationRepository: NavigationRepository = SystemNavigationRepository(application)
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
            initialValue = ResolvedNavigationProvider(
                provider = NavigationProvider.SystemDefault,
                preferredMissing = false,
            ),
        )

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

    /** Phase 18 — locally saved places, newest first. Never leaves the device. */
    val savedPlaces: StateFlow<List<SavedPlace>> = navigationPlacesRepository.savedPlaces
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Phase 18 — destinations recorded only after a successful handoff. */
    val recentDestinations: StateFlow<List<RecentDestination>> =
        navigationPlacesRepository.recentDestinations
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setTheme(themeId: AutoVerseThemeId) {
        viewModelScope.launch {
            settingsRepository.setTheme(themeId)
            diagnosticLog.record(DiagnosticLevel.INFO, "Theme changed to ${themeId.displayName}")
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

    fun setEmbeddedMapEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEmbeddedMapEnabled(enabled)
            diagnosticLog.record(
                DiagnosticLevel.INFO,
                if (enabled) "Embedded map preview enabled" else "Embedded map preview disabled",
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
    fun saveSavedPlace(label: String, destination: NavigationDestination) {
        viewModelScope.launch {
            navigationPlacesRepository.savePlace(label, destination)
            diagnosticLog.record(DiagnosticLevel.INFO, "Saved place added")
        }
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
        if (outcome == LaunchOutcome.NOT_FOUND) {
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

    /**
     * Hands [destination] to the effective navigation provider. Failures come
     * back as outcomes so the caller can explain them; nothing is thrown.
     * Failure events keep the user-facing wording and carry no user content.
     * Phase 18: a handoff that actually opened records the destination in the
     * local recents list — attempts that failed never do.
     */
    suspend fun navigateToDestination(destination: NavigationDestination): NavigationOutcome {
        val resolved = navigationProvider.value
        val outcome = navigationRepository.launch(destination, resolved.provider)
        if (outcome == NavigationOutcome.OPENED) {
            navigationPlacesRepository.recordRecent(destination)
        } else {
            diagnosticLog.record(
                DiagnosticLevel.WARN,
                navigationFailureMessage(resolved.provider.label, outcome),
            )
        }
        return outcome
    }

    /** Opens the effective navigation target without setting a destination. */
    suspend fun openNavigationApp(): NavigationOutcome {
        val resolved = navigationProvider.value
        val outcome = navigationRepository.launch(null, resolved.provider)
        if (outcome != NavigationOutcome.OPENED) {
            diagnosticLog.record(
                DiagnosticLevel.WARN,
                navigationFailureMessage(resolved.provider.label, outcome),
            )
        }
        return outcome
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
    }
}

/** Reads the platform-wide animator duration scale; zero means "removed". */
private fun isSystemAnimationsDisabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
