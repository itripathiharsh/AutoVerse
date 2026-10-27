package com.autoverse.launcher

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.design.components.LocalLiveMapHost
import com.autoverse.launcher.core.design.components.LocalMapEngineState
import com.autoverse.launcher.core.design.components.MapEngineState
import com.autoverse.launcher.core.design.components.rememberLiveMapHost
import com.autoverse.launcher.core.design.tokens.effectiveReducedMotion
import com.autoverse.launcher.core.diagnostics.buildAccessReport
import com.autoverse.launcher.core.media.isNotificationAccessEnabled
import com.autoverse.launcher.core.media.notificationAccessSettingsIntent
import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.performance.FrameTimeTracker
import com.autoverse.launcher.core.performance.LocalPerformanceSpec
import com.autoverse.launcher.core.util.formatClock
import com.autoverse.launcher.core.util.formatFullDate
import com.autoverse.launcher.feature.apps.AppDrawerScreen
import com.autoverse.launcher.feature.diagnostics.DiagnosticsScreen
import com.autoverse.launcher.feature.home.DashboardData
import com.autoverse.launcher.feature.home.HomeScreen
import com.autoverse.launcher.feature.home.HomeScreenContent
import com.autoverse.launcher.feature.navigation.NavigationScreen
import com.autoverse.launcher.feature.obd.ObdDiagnosticsScreen
import com.autoverse.launcher.feature.probe.MapDisplayProbeScreen
import com.autoverse.launcher.feature.probe.MapEmbedProbeScreen
import com.autoverse.launcher.feature.onboarding.OnboardingScreen
import com.autoverse.launcher.feature.settings.SettingsCategory
import com.autoverse.launcher.feature.settings.SettingsScreen
import com.autoverse.launcher.feature.speedometer.SpeedometerScreen
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class Screen { HOME, NAVIGATION, APP_DRAWER, SPEEDOMETER, OBD, SETTINGS, DIAGNOSTICS, MAP_PROBE, MAP_DISPLAY_PROBE }

@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val performanceSpec by viewModel.performanceSpec.collectAsStateWithLifecycle()
    val frameSnapshot by viewModel.frameSnapshot.collectAsStateWithLifecycle()
    val appListState by viewModel.appListState.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val navigationProviders by viewModel.navigationProviders.collectAsStateWithLifecycle()
    val navigationProvider by viewModel.navigationProvider.collectAsStateWithLifecycle()
    val savedPlaces by viewModel.savedPlaces.collectAsStateWithLifecycle()
    val recentDestinations by viewModel.recentDestinations.collectAsStateWithLifecycle()
    val connectivity by viewModel.connectivity.collectAsStateWithLifecycle()
    val systemAnimationsDisabled by viewModel.systemAnimationsDisabled.collectAsStateWithLifecycle()
    val frameHistory by viewModel.frameHistory.collectAsStateWithLifecycle()
    val frameTrackingActive by viewModel.frameTrackingActive.collectAsStateWithLifecycle()
    val systemSnapshot by viewModel.systemSnapshot.collectAsStateWithLifecycle()
    val diagnosticsEvents by viewModel.diagnosticsEvents.collectAsStateWithLifecycle()
    val diagnosticsRefreshing by viewModel.diagnosticsRefreshing.collectAsStateWithLifecycle()
    val settingsStoreReadable by viewModel.settingsStoreReadable.collectAsStateWithLifecycle()
    val speedometerData by viewModel.speedometerData.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val obdState by viewModel.obdState.collectAsStateWithLifecycle()
    val obdPairedAdapters by viewModel.obdPairedAdapters.collectAsStateWithLifecycle()
    val obdDtcState by viewModel.obdDtcState.collectAsStateWithLifecycle()
    val recentApps by viewModel.recentApps.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val mapsStatus by viewModel.mapsStatus.collectAsStateWithLifecycle()

    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    var drawerQuery by rememberSaveable { mutableStateOf("") }
    var drawerCategory by rememberSaveable { mutableStateOf(AppCategory.ALL) }
    var navQuery by rememberSaveable { mutableStateOf("") }
    var settingsCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    val context = LocalContext.current
    val baseDensity = LocalDensity.current
    val scope = rememberCoroutineScope()

    var notificationAccessGranted by remember {
        mutableStateOf(isNotificationAccessEnabled(context))
    }
    var locationGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var phoneGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        // The GPS listener was skipped at foreground time without the grant.
        if (locationGranted) viewModel.startSpeedometer()
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshObdAdapters()
    }

    val phoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // The call-monitor effect below is keyed on this flag, so a grant
        // starts listening and a denial leaves the speed panel in place.
        phoneGranted = granted
    }

    val accessItems = remember(notificationAccessGranted) {
        buildAccessReport(
            Build.VERSION.SDK_INT,
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.INTERNET,
            ) == PackageManager.PERMISSION_GRANTED,
            notificationAccessGranted,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.ensureAppsLoaded()
        viewModel.noteNotificationAccess(notificationAccessGranted)
    }

    // GPS (speed panel, live map, Speed page) runs whenever AutoVerse is
    // started — not tied to one screen — and stops in the background. The
    // location grant is a key so tracking begins the moment it is given.
    val gpsLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(gpsLifecycleOwner, locationGranted) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppForeground()
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackground()
                else -> Unit
            }
        }
        if (gpsLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onAppForeground()
        }
        gpsLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { gpsLifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Call observation runs only while Home is on screen and AutoVerse is
    // started: the platform pushes state changes (and the current state on
    // registration), so stopping in the background loses nothing.
    val callLifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(screen, phoneGranted, callLifecycleOwner) {
        val wanted = screen == Screen.HOME && phoneGranted
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (wanted) viewModel.startCallMonitoring()
                Lifecycle.Event.ON_STOP -> viewModel.stopCallMonitoring()
                else -> Unit
            }
        }
        if (wanted && callLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.startCallMonitoring()
        }
        callLifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            callLifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopCallMonitoring()
        }
    }

    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                viewModel.refreshApps()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshMediaSession()
                viewModel.refreshAppsIfStale()
                // Re-read on every resume so the settings and media surfaces
                // pick up notification-access changes made in system settings.
                val granted = isNotificationAccessEnabled(context)
                notificationAccessGranted = granted
                locationGranted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                phoneGranted =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                        PackageManager.PERMISSION_GRANTED
                // The log records only transitions; repeated resumes with an
                // unchanged state stay silent.
                viewModel.noteNotificationAccess(granted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Frame sampling is wanted only where a readout actually shows: the home
    // dashboard under a profile with the FPS chip, or diagnostics itself.
    val frameSamplingActive = screen == Screen.DIAGNOSTICS ||
        (screen == Screen.HOME && performanceSpec.showFpsReadout)
    FrameSamplingEffect(active = frameSamplingActive, tracker = viewModel.frameTracker)

    // Resource sampling (CPU / memory / storage) runs only on the diagnostics
    // screen, and only while the activity is resumed.
    DiagnosticsSamplingEffect(
        active = screen == Screen.DIAGNOSTICS,
        viewModel = viewModel,
    )

    AutoVerseTheme(
        themeId = settings.themeId,
        appearanceMode = settings.appearanceMode,
        reducedMotion = effectiveReducedMotion(
            userSetting = settings.reducedMotion,
            systemAnimationsDisabled = systemAnimationsDisabled,
        ),
    ) {
        // One live map for the whole session; Home and Maps share it. It is
        // created after Home's first frame, with the engine loaded off-thread.
        var mapEngineState by remember { mutableStateOf(MapEngineState.STARTING) }
        val liveMapHost = rememberLiveMapHost { mapEngineState = it }
        CompositionLocalProvider(
            LocalLiveMapHost provides liveMapHost,
            LocalMapEngineState provides mapEngineState,
            LocalPerformanceSpec provides performanceSpec,
            LocalDensity provides Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * settings.textScale,
            ),
        ) {
            // Always consumed: a launcher's Home has nowhere to go back to, and with
            // predictive back (targetSdk 36) an unhandled gesture would animate Home away.
            BackHandler(enabled = true) { if (screen != Screen.HOME) screen = Screen.HOME }

            if (!settings.onboardingComplete) {
                OnboardingScreen(
                    selectedTheme = settings.themeId,
                    onSelectTheme = { viewModel.setTheme(it) },
                    premiumUnlocked = settings.premiumUnlocked,
                    onUnlockPremium = { viewModel.setPremiumUnlocked(true) },
                    mediaAccessGranted = notificationAccessGranted,
                    onOpenMediaAccess = {
                        openNotificationAccessSettings(context)
                        notificationAccessGranted = isNotificationAccessEnabled(context)
                    },
                    locationGranted = locationGranted,
                    onGetStarted = { viewModel.setOnboardingComplete(true) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else when (screen) {
                    Screen.HOME -> {
                        val dashboard = rememberDashboardData(
                            viewModel = viewModel,
                            settings = settings,
                            frameSnapshot = frameSnapshot,
                            connectivity = connectivity,
                        )
                        HomeScreen(
                            data = dashboard,
                            onOpenSettings = {
                                settingsCategory = null
                                screen = Screen.SETTINGS
                            },
                            onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                            onOpenAppDrawer = {
                                drawerQuery = ""
                                screen = Screen.APP_DRAWER
                            },
                            mediaState = mediaState,
                            onMediaCommand = viewModel::sendMediaCommand,
                            onOpenMediaAccessSettings = {
                                openNotificationAccessSettings(context)
                            },
                            loadMediaArtwork = viewModel::loadMediaArtwork,
                            onRefreshMedia = viewModel::refreshMediaSession,
                            onSkipToQueueItem = viewModel::skipMediaToQueue,
                            mapsStatus = mapsStatus,
                            destinationTargetLabel = navigationProvider.provider.label,
                            onNavigateToDestination = viewModel::navigateToDestination,
                            onOpenGoogleMaps = viewModel::openGoogleMaps,
                            onLaunchVerdict = viewModel::recordLaunchVerdict,
                            onOpenNavigation = { screen = Screen.NAVIGATION },
                            onOpenSpeedometer = { screen = Screen.SPEEDOMETER },
                            onOpenObd = { screen = Screen.OBD },
                            recentDestinations = recentDestinations,
                            speedometerData = speedometerData,
                            weatherState = weatherState,
                            hasLocationPermission = locationGranted,
                            onRequestLocationPermission = {
                                locationLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            },
                            callState = callState,
                            hasCallPermission = phoneGranted,
                            onRequestCallPermission = {
                                phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                            },
                            mapsDestinationQuery = navQuery,
                            onMapsDestinationQueryChange = { navQuery = it },
                        )
                    }

                    Screen.NAVIGATION -> {
                        val dashboard = rememberDashboardData(
                            viewModel = viewModel,
                            settings = settings,
                            frameSnapshot = frameSnapshot,
                            connectivity = connectivity,
                        )
                        NavigationScreen(
                            clockText = dashboard.clockText,
                            dateText = dashboard.dateText,
                            connectivity = dashboard.connectivity,
                            mapsStatus = mapsStatus,
                            destinationTargetLabel = navigationProvider.provider.label,
                            savedPlaces = savedPlaces,
                            speedometerData = speedometerData,
                            hasLocationPermission = locationGranted,
                            recentDestinations = recentDestinations,
                            query = navQuery,
                            onQueryChange = { navQuery = it },
                            onNavigate = viewModel::navigateToDestination,
                            onOpenGoogleMaps = viewModel::openGoogleMaps,
                            onLaunchVerdict = viewModel::recordLaunchVerdict,
                            onSavePlace = viewModel::saveSavedPlace,
                            onRemovePlace = viewModel::removeSavedPlace,
                            onClearRecents = viewModel::clearRecentDestinations,
                            placesCollapsed = settings.mapsPlacesCollapsed,
                            onPlacesCollapsedChange = viewModel::setMapsPlacesCollapsed,
                            onOpenHome = { screen = Screen.HOME },
                            onOpenAppDrawer = {
                                drawerQuery = ""
                                screen = Screen.APP_DRAWER
                            },
                            onOpenSpeedometer = { screen = Screen.SPEEDOMETER },
                            onOpenObd = { screen = Screen.OBD },
                            onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                            onOpenSettings = {
                                settingsCategory = null
                                screen = Screen.SETTINGS
                            },
                        )
                    }

                    Screen.SPEEDOMETER -> {
                        SpeedometerScreen(
                            speedometerData = speedometerData,
                            weatherState = weatherState,
                            onResetTrip = viewModel::resetTrip,
                            onRefreshWeather = viewModel::refreshWeather,
                            onOpenHome = { screen = Screen.HOME },
                            onOpenAppDrawer = {
                                drawerQuery = ""
                                screen = Screen.APP_DRAWER
                            },
                            onOpenNavigation = { screen = Screen.NAVIGATION },
                            onOpenObd = { screen = Screen.OBD },
                            onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                            onOpenSettings = {
                                settingsCategory = null
                                screen = Screen.SETTINGS
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Screen.OBD -> {
                        ObdDiagnosticsScreen(
                            obdState = obdState,
                            pairedAdapters = obdPairedAdapters,
                            dtcState = obdDtcState,
                            onRefreshAdapters = viewModel::refreshObdAdapters,
                            onConnectObd = viewModel::connectObd,
                            onDisconnectObd = viewModel::disconnectObd,
                            onRequestDtcs = viewModel::requestDtcs,
                            onRequestBluetoothPermission = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    bluetoothLauncher.launch(
                                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                                    )
                                }
                            },
                            onOpenHome = { screen = Screen.HOME },
                            onOpenAppDrawer = {
                                drawerQuery = ""
                                screen = Screen.APP_DRAWER
                            },
                            onOpenNavigation = { screen = Screen.NAVIGATION },
                            onOpenSpeedometer = { screen = Screen.SPEEDOMETER },
                            onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                            onOpenSettings = {
                                settingsCategory = null
                                screen = Screen.SETTINGS
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Screen.APP_DRAWER -> {
                        val dashboard = rememberDashboardData(
                            viewModel = viewModel,
                            settings = settings,
                            frameSnapshot = frameSnapshot,
                            connectivity = connectivity,
                        )
                        AppDrawerScreen(
                            clockText = dashboard.clockText,
                            dateText = dashboard.dateText,
                            connectivity = dashboard.connectivity,
                            state = appListState,
                            searchQuery = drawerQuery,
                            onSearchQueryChange = { drawerQuery = it },
                            category = drawerCategory,
                            onCategoryChange = { drawerCategory = it },
                            onLaunchApp = viewModel::launchApp,
                            onRefresh = viewModel::refreshApps,
                            loadIcon = viewModel::loadAppIcon,
                            onOpenHome = { screen = Screen.HOME },
                            onOpenNavigation = { screen = Screen.NAVIGATION },
                            onOpenSpeedometer = { screen = Screen.SPEEDOMETER },
                            onOpenObd = { screen = Screen.OBD },
                            recentApps = recentApps,
                            recentCollapsed = settings.appsRecentCollapsed,
                            onRecentCollapsedChange = viewModel::setAppsRecentCollapsed,
                            onClearRecent = viewModel::clearRecentApps,
                            onOpenSettings = {
                                settingsCategory = null
                                screen = Screen.SETTINGS
                            },
                            onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                        )
                    }

                    Screen.SETTINGS -> {
                        val dashboard = rememberDashboardData(
                            viewModel = viewModel,
                            settings = settings,
                            frameSnapshot = frameSnapshot,
                            connectivity = connectivity,
                        )
                        SettingsScreen(
                            settings = settings,
                            initialCategory = settingsCategory,
                            onSetTheme = viewModel::setTheme,
                            onSetPremiumUnlocked = viewModel::setPremiumUnlocked,
                            onSetThemeSoundsEnabled = viewModel::setThemeSoundsEnabled,
                            onReplayOnboarding = { viewModel.setOnboardingComplete(false) },
                            onSetAppearanceMode = viewModel::setAppearanceMode,
                            onSetReducedMotion = viewModel::setReducedMotion,
                            onSetTextScale = viewModel::setTextScale,
                            onSetProfile = viewModel::setProfile,
                            onSetPreferSmoothness = viewModel::setPreferSmoothness,
                            onSetClock24h = viewModel::setClock24h,
                            onBack = { screen = Screen.HOME },
                            navigationProviders = navigationProviders,
                            onSetNavigationPackage = viewModel::setNavigationPackage,
                            notificationAccessGranted = notificationAccessGranted,
                            onOpenNotificationAccessSettings = {
                                openNotificationAccessSettings(context)
                            },
                            favoriteApps = favoriteApps,
                            onLaunchApp = { app -> scope.launch { viewModel.launchApp(app) } },
                            onRemoveFavorite = { app -> viewModel.toggleFavorite(app.key) },
                            onOpenAppDrawer = {
                                drawerQuery = ""
                                screen = Screen.APP_DRAWER
                            },
                            loadAppIcon = viewModel::loadAppIcon,
                            onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                            onOpenHome = { screen = Screen.HOME },
                            onOpenNavigation = { screen = Screen.NAVIGATION },
                            onOpenSpeedometer = { screen = Screen.SPEEDOMETER },
                            onOpenObd = { screen = Screen.OBD },
                            clockText = dashboard.clockText,
                            dateText = dashboard.dateText,
                            connectivity = dashboard.connectivity,
                            dashboardPreview = { previewModifier ->
                                DashboardPreview(data = dashboard, modifier = previewModifier)
                            },
                            onResetSettings = viewModel::resetSettings,
                        )
                    }

                    Screen.DIAGNOSTICS -> {
                        val dashboard = rememberDashboardData(
                            viewModel = viewModel,
                            settings = settings,
                            frameSnapshot = frameSnapshot,
                            connectivity = connectivity,
                        )
                        DiagnosticsScreen(
                            clockText = dashboard.clockText,
                            dateText = dashboard.dateText,
                            connectivity = dashboard.connectivity,
                            profile = viewModel.deviceProfile,
                            selectedProfile = settings.profile,
                            frameSnapshot = frameSnapshot,
                            frameHistory = frameHistory,
                            frameTrackingActive = frameTrackingActive,
                            systemSnapshot = systemSnapshot,
                            diagnosticsEvents = diagnosticsEvents,
                            appListState = appListState,
                            mediaState = mediaState,
                            navigationProvider = navigationProvider,
                            settingsStoreReadable = settingsStoreReadable,
                            accessItems = accessItems,
                            notificationAccessGranted = notificationAccessGranted,
                            settings = settings,
                            systemAnimationsDisabled = systemAnimationsDisabled,
                            refreshing = diagnosticsRefreshing,
                            onRefresh = viewModel::refreshDiagnostics,
                            onClearLog = viewModel::clearDiagnosticsLog,
                            onBack = { screen = Screen.HOME },
                            onOpenHome = { screen = Screen.HOME },
                            onOpenAppDrawer = {
                                drawerQuery = ""
                                screen = Screen.APP_DRAWER
                            },
                            onOpenNavigation = { screen = Screen.NAVIGATION },
                            onOpenSpeedometer = { screen = Screen.SPEEDOMETER },
                            onOpenObd = { screen = Screen.OBD },
                            onOpenSettings = {
                                settingsCategory = null
                                screen = Screen.SETTINGS
                            },
                            onOpenAppearanceSettings = {
                                settingsCategory = SettingsCategory.APPEARANCE
                                screen = Screen.SETTINGS
                            },
                            onOpenPerformanceSettings = {
                                settingsCategory = SettingsCategory.PERFORMANCE
                                screen = Screen.SETTINGS
                            },
                            onOpenPermissionsSettings = {
                                settingsCategory = SettingsCategory.MEDIA_AUDIO
                                screen = Screen.SETTINGS
                            },
                            onOpenMapProbe = { screen = Screen.MAP_PROBE },
                            onOpenMapDisplayProbe = { screen = Screen.MAP_DISPLAY_PROBE },
                        )
                    }

                    Screen.MAP_PROBE -> {
                        MapEmbedProbeScreen(
                            onBack = { screen = Screen.DIAGNOSTICS },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Screen.MAP_DISPLAY_PROBE -> {
                        MapDisplayProbeScreen(
                            onBack = { screen = Screen.DIAGNOSTICS },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
            }
        }
    }
}

/**
 * Dashboard clock/date/FPS/connectivity bundle shared by the Home dashboard
 * and the drawer's top strip, so both screens render the identical live data
 * from one place instead of drifting copies.
 */
@Composable
private fun rememberDashboardData(
    viewModel: MainViewModel,
    settings: AppSettings,
    frameSnapshot: FrameTimeTracker.Snapshot,
    connectivity: ConnectivityStatus,
): DashboardData {
    val now by viewModel.now.collectAsStateWithLifecycle()
    return remember(now, settings, frameSnapshot, connectivity) {
        DashboardData(
            clockText = formatClock(now.toLocalTime(), settings.clock24h),
            dateText = formatFullDate(now.toLocalDate()),
            fpsText = "${frameSnapshot.fps.roundToInt()} FPS",
            connectivity = connectivity,
        )
    }
}

/**
 * Runs [tracker] only while [active] and the activity is RESUMED, so the FPS
 * readout never samples a backgrounded or off-screen surface and the sampler
 * costs nothing on screens with no readout.
 */
@Composable
private fun FrameSamplingEffect(active: Boolean, tracker: FrameTimeTracker) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, tracker, active) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && active) {
                tracker.start()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                tracker.stop()
            }
        }
        if (active && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            tracker.start()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            tracker.stop()
        }
    }
}

/**
 * Runs the diagnostics resource sampler ([MainViewModel.setDiagnosticsSampling])
 * only while the diagnostics screen is visible and the activity is RESUMED, so
 * CPU / memory / storage reads happen only when someone can see them. The last
 * reading survives a pause so the panel never blanks mid-session.
 */
@Composable
private fun DiagnosticsSamplingEffect(active: Boolean, viewModel: MainViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel, active) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && active) {
                viewModel.setDiagnosticsSampling(true)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.setDiagnosticsSampling(false)
            }
        }
        if (active && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            viewModel.setDiagnosticsSampling(true)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.setDiagnosticsSampling(false)
        }
    }
}

/**
 * Renders the real cockpit composition as the settings live preview, with
 * zero insets and inert sections: adjusting a setting here must never launch
 * an app, hand off navigation, or open a media surface by accident. The
 * cockpit geometry is computed from the preview box itself, so the miniature
 * adapts exactly as Home would at that size.
 */
@Composable
private fun DashboardPreview(
    data: DashboardData,
    modifier: Modifier = Modifier,
) {
    // The miniature must never adopt the session's single live MapView.
    CompositionLocalProvider(LocalLiveMapHost provides null) {
        HomeScreenContent(
            data = data,
            contentInsets = WindowInsets(0, 0, 0, 0),
            modifier = modifier.fillMaxSize(),
        )
    }
}

/**
 * Opens the system notification-access settings screen. Returns false when no
 * activity resolves (some OEM ROMs) so the UI can explain instead of failing
 * silently.
 */
private fun openNotificationAccessSettings(context: Context): Boolean {
    val intent = notificationAccessSettingsIntent()
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
