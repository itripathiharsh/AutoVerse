package com.autoverse.launcher

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.graphics.ImageBitmap
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
import com.autoverse.launcher.core.design.homeLayoutPolicyFor
import com.autoverse.launcher.core.design.tokens.LocalAutoVerseMotion
import com.autoverse.launcher.core.design.tokens.effectiveReducedMotion
import com.autoverse.launcher.core.diagnostics.buildAccessReport
import com.autoverse.launcher.core.media.isNotificationAccessEnabled
import com.autoverse.launcher.core.media.notificationAccessSettingsIntent
import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.navigationPageProvider
import com.autoverse.launcher.core.performance.FrameTimeTracker
import com.autoverse.launcher.core.performance.LocalPerformanceSpec
import com.autoverse.launcher.core.performance.ScreenTransitionStyle
import com.autoverse.launcher.core.util.formatClock
import com.autoverse.launcher.core.util.formatFullDate
import com.autoverse.launcher.feature.apps.AppDrawerScreen
import com.autoverse.launcher.feature.apps.AppDrawerView
import com.autoverse.launcher.feature.diagnostics.DiagnosticsScreen
import com.autoverse.launcher.feature.home.DashboardData
import com.autoverse.launcher.feature.home.HomeScreen
import com.autoverse.launcher.feature.home.HomeScreenContent
import com.autoverse.launcher.feature.map.MapPocScreen
import com.autoverse.launcher.feature.navigation.NavigationScreen
import com.autoverse.launcher.feature.settings.SettingsCategory
import com.autoverse.launcher.feature.settings.SettingsScreen
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class Screen { HOME, NAVIGATION, APP_DRAWER, SETTINGS, DIAGNOSTICS, MAP_POC }

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

    var screen by remember { mutableStateOf(Screen.HOME) }
    var drawerQuery by rememberSaveable { mutableStateOf("") }
    var drawerView by rememberSaveable { mutableStateOf(AppDrawerView.GRID) }
    var drawerCategory by rememberSaveable { mutableStateOf(AppCategory.ALL) }
    var navQuery by rememberSaveable { mutableStateOf("") }
    var navPlacesPanelVisible by rememberSaveable { mutableStateOf(true) }
    var settingsCategory by remember { mutableStateOf<SettingsCategory?>(null) }

    val context = LocalContext.current
    val baseDensity = LocalDensity.current
    val scope = rememberCoroutineScope()

    var notificationAccessGranted by remember {
        mutableStateOf(isNotificationAccessEnabled(context))
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
                // Re-read on every resume so the settings and media surfaces
                // pick up notification-access changes made in system settings.
                val granted = isNotificationAccessEnabled(context)
                notificationAccessGranted = granted
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
        CompositionLocalProvider(
            LocalPerformanceSpec provides performanceSpec,
            LocalDensity provides Density(
                density = baseDensity.density,
                fontScale = baseDensity.fontScale * settings.textScale,
            ),
        ) {
            val motion = LocalAutoVerseMotion.current
            BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    when (performanceSpec.screenTransitions) {
                        ScreenTransitionStyle.NONE ->
                            EnterTransition.None togetherWith ExitTransition.None
                        ScreenTransitionStyle.FADE ->
                            fadeIn(tween(motion.standardMs)) togetherWith
                                fadeOut(tween(motion.fastMs))
                        ScreenTransitionStyle.RICH ->
                            (fadeIn(tween(motion.standardMs)) +
                                slideInVertically(tween(motion.standardMs)) { height -> height / 32 }) togetherWith
                                fadeOut(tween(motion.fastMs))
                    }
                },
                label = "screenTransition",
            ) { target ->
                when (target) {
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
                            favoriteApps = favoriteApps,
                            onLaunchApp = viewModel::launchApp,
                            onOpenAppDrawer = {
                                drawerQuery = ""
                                screen = Screen.APP_DRAWER
                            },
                            loadAppIcon = viewModel::loadAppIcon,
                            mediaState = mediaState,
                            onMediaCommand = viewModel::sendMediaCommand,
                            onOpenMediaAccessSettings = {
                                openNotificationAccessSettings(context)
                            },
                            loadMediaArtwork = viewModel::loadMediaArtwork,
                            onRefreshMedia = viewModel::refreshMediaSession,
                            navigationProvider = navigationProvider,
                            onNavigateToDestination = viewModel::navigateToDestination,
                            onOpenNavigationApp = viewModel::openNavigationApp,
                            onOpenNavigation = { screen = Screen.NAVIGATION },
                            selectedProfile = settings.profile,
                            onSelectProfile = viewModel::setProfile,
                        )
                    }

                    Screen.NAVIGATION -> {
                        val pageResolved = remember(navigationProvider, navigationProviders) {
                            navigationPageProvider(navigationProvider, navigationProviders)
                        }
                        NavigationScreen(
                            resolved = pageResolved,
                            savedPlaces = savedPlaces,
                            recentDestinations = recentDestinations,
                            embeddedMapEnabled = settings.embeddedMapEnabled,
                            query = navQuery,
                            onQueryChange = { navQuery = it },
                            placesPanelVisible = navPlacesPanelVisible,
                            onTogglePlacesPanel = { navPlacesPanelVisible = !navPlacesPanelVisible },
                            onNavigate = viewModel::navigateToDestination,
                            onOpenNavigationApp = viewModel::openNavigationApp,
                            onSavePlace = viewModel::saveSavedPlace,
                            onRemovePlace = viewModel::removeSavedPlace,
                            onChangeProvider = {
                                settingsCategory = SettingsCategory.NAVIGATION
                                screen = Screen.SETTINGS
                            },
                            onBack = { screen = Screen.HOME },
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
                            favorites = favorites,
                            searchQuery = drawerQuery,
                            onSearchQueryChange = { drawerQuery = it },
                            viewMode = drawerView,
                            onViewModeChange = { drawerView = it },
                            category = drawerCategory,
                            onCategoryChange = { drawerCategory = it },
                            onToggleFavorite = { app -> viewModel.toggleFavorite(app.key) },
                            onSetFavorites = viewModel::setFavorites,
                            onLaunchApp = viewModel::launchApp,
                            onRefresh = viewModel::refreshApps,
                            loadIcon = viewModel::loadAppIcon,
                            onBack = { screen = Screen.HOME },
                            onOpenHome = { screen = Screen.HOME },
                            onOpenSettings = {
                                settingsCategory = null
                                screen = Screen.SETTINGS
                            },
                            onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                            onOpenNavigation = { screen = Screen.NAVIGATION },
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
                            onSetAppearanceMode = viewModel::setAppearanceMode,
                            onSetReducedMotion = viewModel::setReducedMotion,
                            onSetTextScale = viewModel::setTextScale,
                            onSetProfile = viewModel::setProfile,
                            onSetPreferSmoothness = viewModel::setPreferSmoothness,
                            onSetClock24h = viewModel::setClock24h,
                            onBack = { screen = Screen.HOME },
                            navigationProviders = navigationProviders,
                            onSetNavigationPackage = viewModel::setNavigationPackage,
                            onSetEmbeddedMapEnabled = viewModel::setEmbeddedMapEnabled,
                            onOpenMapPreview = { screen = Screen.MAP_POC },
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
                            clockText = dashboard.clockText,
                            dateText = dashboard.dateText,
                            connectivity = dashboard.connectivity,
                            dashboardPreview = { previewModifier ->
                                DashboardPreview(
                                    data = dashboard,
                                    selectedProfile = settings.profile,
                                    favoriteApps = favoriteApps,
                                    loadAppIcon = viewModel::loadAppIcon,
                                    modifier = previewModifier,
                                )
                            },
                            onResetSettings = viewModel::resetSettings,
                        )
                    }

                    Screen.MAP_POC -> {
                        if (settings.embeddedMapEnabled) {
                            MapPocScreen(
                                providerLabel = navigationProvider.provider.label,
                                onOpenNavigationApp = viewModel::openNavigationApp,
                                onBack = { screen = Screen.SETTINGS },
                            )
                        } else {
                            LaunchedEffect(Unit) { screen = Screen.SETTINGS }
                        }
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
                        )
                    }
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
 * Renders the real dashboard composition as the settings live preview. It
 * reuses [HomeScreenContent] with the same live data the Home screen renders,
 * but with zero insets and inert callbacks: adjusting a setting here must
 * never launch an app, hand off navigation, or open a media surface by
 * accident. The policy is computed from the preview box itself, so the
 * miniature adapts exactly as the full dashboard would at that size. Frame
 * sampling stays scoped to screens that show an FPS readout — the miniature
 * is too small for status chips, so the settings preview adds no sampling.
 */
@Composable
private fun DashboardPreview(
    data: DashboardData,
    selectedProfile: PerformanceProfile,
    favoriteApps: List<InstalledApp>,
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val policy = homeLayoutPolicyFor(
            widthDp = maxWidth.value.roundToInt(),
            heightDp = maxHeight.value.roundToInt(),
            fontScale = LocalDensity.current.fontScale,
        )
        HomeScreenContent(
            data = data,
            policy = policy,
            onOpenSettings = {},
            onOpenDiagnostics = {},
            contentInsets = WindowInsets(0, 0, 0, 0),
            favoriteApps = favoriteApps,
            loadAppIcon = loadAppIcon,
            selectedProfile = selectedProfile,
            modifier = Modifier.fillMaxSize(),
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
