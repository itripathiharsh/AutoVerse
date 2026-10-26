package com.autoverse.launcher.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.CockpitArrangement
import com.autoverse.launcher.core.design.CockpitGeometry
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.StatusChipsMode
import com.autoverse.launcher.core.design.cockpitGeometryFor
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.DashboardTopBar
import com.autoverse.launcher.core.design.components.IncomingCallCard
import com.autoverse.launcher.core.design.components.MapsLaunchPanel
import com.autoverse.launcher.core.design.components.MediaCard
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.SpeedSummaryCard
import com.autoverse.launcher.core.design.components.rememberNavigationLaunchFeedback
import com.autoverse.launcher.core.design.homeLayoutPolicyFor
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.media.MediaCommand
import com.autoverse.launcher.core.media.MediaCommandOutcome
import com.autoverse.launcher.core.media.MediaQueueUi
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.mediaCommandFailureMessage
import com.autoverse.launcher.core.media.mediaPresentation
import com.autoverse.launcher.core.media.mediaToggleCommand
import com.autoverse.launcher.core.navigation.MapsForegroundVerdict
import com.autoverse.launcher.core.navigation.MapsStatus
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.phone.CallUiState
import com.autoverse.launcher.core.phone.showsIncomingCall
import com.autoverse.launcher.core.weather.WeatherUiState
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class DashboardData(
    val clockText: String,
    val dateText: String,
    val fpsText: String,
    val connectivity: ConnectivityStatus = ConnectivityStatus(),
)

/**
 * Media wiring for the cockpit's music panel. Defaults render the no-session
 * surface, so previews and tests work without a media layer.
 */
internal data class MediaSection(
    val state: MediaUiState = MediaUiState.NoSession,
    val loadArtwork: suspend (MediaUiState) -> ImageBitmap? = { null },
    val onCommand: (MediaCommand) -> Unit = {},
    val onSkipToQueue: (Long) -> Unit = {},
    val onOpenAccess: () -> Unit = {},
    val onBrowseApps: () -> Unit = {},
    val onRefresh: () -> Unit = {},
)

/** Map-region wiring; inert defaults for previews. */
internal data class MapsSection(
    val status: MapsStatus = MapsStatus.UNKNOWN,
    val destinationTargetLabel: String = "Google Maps",
    val query: String = "",
    val onQueryChange: (String) -> Unit = {},
    val onNavigate: (NavigationDestination) -> Unit = {},
    val onOpenMaps: () -> Unit = {},
    val recentDestinations: List<RecentDestination> = emptyList(),
)

/** Speed / call panel wiring; inert defaults for previews. */
internal data class DrivingSection(
    val speedometerData: SpeedometerData = SpeedometerData(),
    val weatherState: WeatherUiState = WeatherUiState.Loading,
    val hasLocationPermission: Boolean = true,
    val onRequestLocationPermission: () -> Unit = {},
    val callState: CallUiState = CallUiState.Idle,
    val hasCallPermission: Boolean = false,
    val onRequestCallPermission: () -> Unit = {},
    val onOpenSpeedometer: () -> Unit = {},
)

/** Rail callbacks; Home itself is the selected entry. */
internal data class RailSection(
    val onOpenAppDrawer: () -> Unit = {},
    val onOpenNavigation: () -> Unit = {},
    val onOpenSpeedometer: () -> Unit = {},
    val onOpenObd: () -> Unit = {},
    val onOpenDiagnostics: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
)

/**
 * Home is the cockpit — map region, music, speedometer — on every window
 * size. There is no card grid and no favourites/driving-status panels.
 */
@Composable
fun HomeScreen(
    data: DashboardData,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenAppDrawer: () -> Unit = {},
    onOpenNavigation: () -> Unit = {},
    onOpenSpeedometer: () -> Unit = {},
    onOpenObd: () -> Unit = {},
    mediaState: MediaUiState = MediaUiState.NoSession,
    onMediaCommand: suspend (MediaCommand) -> MediaCommandOutcome = { MediaCommandOutcome.SENT },
    onSkipToQueueItem: suspend (Long) -> MediaCommandOutcome = { MediaCommandOutcome.SENT },
    onOpenMediaAccessSettings: () -> Boolean = { false },
    loadMediaArtwork: suspend (MediaUiState) -> ImageBitmap? = { null },
    onRefreshMedia: () -> Unit = {},
    mapsStatus: MapsStatus = MapsStatus.UNKNOWN,
    destinationTargetLabel: String = "Google Maps",
    onNavigateToDestination: suspend (NavigationDestination) -> NavigationOutcome = { NavigationOutcome.REQUESTED },
    onOpenGoogleMaps: suspend () -> NavigationOutcome = { NavigationOutcome.REQUESTED },
    onLaunchVerdict: (String, MapsForegroundVerdict) -> Unit = { _, _ -> },
    recentDestinations: List<RecentDestination> = emptyList(),
    mapsDestinationQuery: String = "",
    onMapsDestinationQueryChange: (String) -> Unit = {},
    speedometerData: SpeedometerData = SpeedometerData(),
    weatherState: WeatherUiState = WeatherUiState.Loading,
    hasLocationPermission: Boolean = true,
    onRequestLocationPermission: () -> Unit = {},
    callState: CallUiState = CallUiState.Idle,
    hasCallPermission: Boolean = false,
    onRequestCallPermission: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val launchFeedback = rememberNavigationLaunchFeedback(snackbarHostState, onLaunchVerdict)

    val reportMediaOutcome: (MediaCommandOutcome) -> Unit = { outcome ->
        if (outcome != MediaCommandOutcome.SENT) {
            scope.launch {
                snackbarHostState.showSnackbar(mediaCommandFailureMessage(outcome), withDismissAction = true)
            }
        }
    }
    val media = MediaSection(
        state = mediaState,
        loadArtwork = loadMediaArtwork,
        onCommand = { command -> scope.launch { reportMediaOutcome(onMediaCommand(command)) } },
        onSkipToQueue = { id -> scope.launch { reportMediaOutcome(onSkipToQueueItem(id)) } },
        onOpenAccess = {
            if (!onOpenMediaAccessSettings()) {
                scope.launch {
                    snackbarHostState.showSnackbar("Couldn't open system settings", withDismissAction = true)
                }
            }
        },
        onBrowseApps = onOpenAppDrawer,
        onRefresh = onRefreshMedia,
    )
    val maps = MapsSection(
        status = mapsStatus,
        destinationTargetLabel = destinationTargetLabel,
        query = mapsDestinationQuery,
        onQueryChange = onMapsDestinationQueryChange,
        onNavigate = { destination ->
            launchFeedback.run(destinationTargetLabel) { onNavigateToDestination(destination) }
        },
        onOpenMaps = { launchFeedback.run("Google Maps", onOpenGoogleMaps) },
        recentDestinations = recentDestinations,
    )
    val driving = DrivingSection(
        speedometerData = speedometerData,
        weatherState = weatherState,
        hasLocationPermission = hasLocationPermission,
        onRequestLocationPermission = onRequestLocationPermission,
        callState = callState,
        hasCallPermission = hasCallPermission,
        onRequestCallPermission = onRequestCallPermission,
        onOpenSpeedometer = onOpenSpeedometer,
    )
    val rail = RailSection(
        onOpenAppDrawer = onOpenAppDrawer,
        onOpenNavigation = onOpenNavigation,
        onOpenSpeedometer = onOpenSpeedometer,
        onOpenObd = onOpenObd,
        onOpenDiagnostics = onOpenDiagnostics,
        onOpenSettings = onOpenSettings,
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        HomeScreenContent(
            data = data,
            media = media,
            maps = maps,
            driving = driving,
            rail = rail,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

/**
 * Stateless cockpit composition. [contentInsets] is applied once; the
 * settings live preview passes zero insets and inert sections.
 */
@Composable
internal fun HomeScreenContent(
    data: DashboardData,
    modifier: Modifier = Modifier,
    contentInsets: WindowInsets = WindowInsets.safeDrawing,
    media: MediaSection = MediaSection(),
    maps: MapsSection = MapsSection(),
    driving: DrivingSection = DrivingSection(),
    rail: RailSection = RailSection(),
) {
    val colors = LocalAutoVerseColors.current
    BoxWithConstraints(modifier = modifier.background(colors.backgroundTop)) {
        val chips = homeLayoutPolicyFor(
            widthDp = maxWidth.value.roundToInt(),
            heightDp = maxHeight.value.roundToInt(),
            fontScale = LocalDensity.current.fontScale,
        ).statusChips
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(contentInsets)
                .padding(horizontal = AutoVerseSpacing.md, vertical = AutoVerseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            DashboardTopBar(
                clockText = data.clockText,
                dateText = data.dateText,
                connectivity = data.connectivity,
                chips = chips.coerceAtMost(StatusChipsMode.REDUCED),
                fpsText = data.fpsText,
                onOpenDiagnostics = rail.onOpenDiagnostics,
                onOpenSettings = rail.onOpenSettings,
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                AppNavigationRail(
                    selected = RailDestination.HOME,
                    onOpenHome = {},
                    onOpenAppDrawer = rail.onOpenAppDrawer,
                    onOpenNavigation = rail.onOpenNavigation,
                    onOpenSpeedometer = rail.onOpenSpeedometer,
                    onOpenObd = rail.onOpenObd,
                    onOpenDiagnostics = rail.onOpenDiagnostics,
                    onOpenSettings = rail.onOpenSettings,
                    modifier = Modifier
                        .width(AutoVerseSpacing.railWidth)
                        .fillMaxHeight(),
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    val geometry = cockpitGeometryFor(
                        contentWidthDp = maxWidth.value.roundToInt(),
                        contentHeightDp = maxHeight.value.roundToInt(),
                    )
                    CockpitRegions(geometry = geometry, media = media, maps = maps, driving = driving)
                }
            }
        }
    }
}

private fun StatusChipsMode.coerceAtMost(limit: StatusChipsMode): StatusChipsMode =
    if (ordinal > limit.ordinal) limit else this

/**
 * Map ~60%, music ~20%, speed/call ~20% of the content area. SIDE_BY_SIDE puts
 * music above speed in a 40% column beside the map; STACKED puts the map on
 * top and music beside speed below it. Nothing animates between them.
 */
@Composable
private fun CockpitRegions(
    geometry: CockpitGeometry,
    media: MediaSection,
    maps: MapsSection,
    driving: DrivingSection,
) {
    val mapWeight = geometry.mapFraction
    val sideWeight = 1f - geometry.mapFraction
    val mapRegion: @Composable (Modifier) -> Unit = { regionModifier ->
        MapsLaunchPanel(
            mapsStatus = maps.status,
            destinationTargetLabel = maps.destinationTargetLabel,
            destinationQuery = maps.query,
            onDestinationQueryChange = maps.onQueryChange,
            onNavigate = maps.onNavigate,
            onOpenMaps = maps.onOpenMaps,
            recentDestinations = maps.recentDestinations,
            compact = geometry.compactPanels,
            speedometerData = driving.speedometerData,
            hasLocationPermission = driving.hasLocationPermission,
            modifier = regionModifier,
        )
    }
    val musicPanel: @Composable (Modifier) -> Unit = { panelModifier ->
        MusicPanel(media = media, dense = true, modifier = panelModifier)
    }
    val speedPanel: @Composable (Modifier) -> Unit = { panelModifier ->
        SpeedOrCallPanel(driving = driving, compact = geometry.compactPanels, modifier = panelModifier)
    }
    when (geometry.arrangement) {
        CockpitArrangement.SIDE_BY_SIDE -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
        ) {
            mapRegion(Modifier.weight(mapWeight).fillMaxHeight())
            Column(
                modifier = Modifier.weight(sideWeight).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                musicPanel(Modifier.weight(1f).fillMaxWidth())
                speedPanel(Modifier.weight(1f).fillMaxWidth())
            }
        }
        CockpitArrangement.STACKED -> Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
        ) {
            mapRegion(Modifier.weight(mapWeight).fillMaxWidth())
            Row(
                modifier = Modifier.weight(sideWeight).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                musicPanel(Modifier.weight(1f).fillMaxHeight())
                speedPanel(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

/** The speed panel, temporarily replaced by the call panel while the platform reports a call. */
@Composable
private fun SpeedOrCallPanel(driving: DrivingSection, compact: Boolean, modifier: Modifier) {
    if (showsIncomingCall(driving.callState, driving.hasCallPermission)) {
        IncomingCallCard(callState = driving.callState, compact = true, modifier = modifier)
    } else {
        SpeedSummaryCard(
            speedometerData = driving.speedometerData,
            hasLocationPermission = driving.hasLocationPermission,
            onOpenSpeedometer = driving.onOpenSpeedometer,
            onRequestLocationPermission = driving.onRequestLocationPermission,
            weatherState = driving.weatherState,
            compact = compact,
            hasCallPermission = driving.hasCallPermission,
            onRequestCallPermission = driving.onRequestCallPermission,
            modifier = modifier,
        )
    }
}

@Composable
private fun MusicPanel(media: MediaSection, dense: Boolean, modifier: Modifier) {
    val presentation = remember(media.state) { mediaPresentation(media.state) }
    val artwork = rememberMediaArtwork(state = media.state, loadArtwork = media.loadArtwork)
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val active = media.state as? MediaUiState.Active
    val queue = active?.takeIf { it.queue.isNotEmpty() }?.let {
        MediaQueueUi(entries = it.queue, total = it.queueTotal, activeQueueId = it.activeQueueId)
    }
    MediaCard(
        presentation = presentation,
        modifier = modifier,
        artwork = artwork,
        dense = dense,
        queue = queue,
        queueTitle = active?.queueTitle,
        showQueue = showQueue && queue != null,
        onViewChange = { showQueue = it },
        onSkipToQueue = media.onSkipToQueue,
        onPrevious = { media.onCommand(MediaCommand.PREVIOUS) },
        onTogglePlay = { media.onCommand(mediaToggleCommand(presentation.status)) },
        onNext = { media.onCommand(MediaCommand.NEXT) },
        onOpenAccessSettings = media.onOpenAccess,
        onBrowseApps = media.onBrowseApps,
        onRetry = media.onRefresh,
    )
}

/**
 * Loads artwork for the current state; resets while a new state's artwork is
 * in flight so a track change never shows the previous track's cover.
 */
@Composable
private fun rememberMediaArtwork(
    state: MediaUiState,
    loadArtwork: suspend (MediaUiState) -> ImageBitmap?,
): ImageBitmap? {
    val artwork by produceState<ImageBitmap?>(initialValue = null, state) {
        value = null
        value = loadArtwork(state)
    }
    return artwork
}
