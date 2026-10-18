package com.autoverse.launcher.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.HomeArchetype
import com.autoverse.launcher.core.design.HomeLayoutPolicy
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.NavigationDensity
import com.autoverse.launcher.core.design.components.AppDockItem
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.CompactNavigationRow
import com.autoverse.launcher.core.design.components.DashboardTopBar
import com.autoverse.launcher.core.design.components.DriveModeCard
import com.autoverse.launcher.core.design.components.MediaCard
import com.autoverse.launcher.core.design.components.NavigationCard
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.TransportButton
import com.autoverse.launcher.core.design.components.TransportVariant
import com.autoverse.launcher.core.design.homeLayoutPolicyFor
import com.autoverse.launcher.core.design.tokens.AutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.design.tokens.LocalAutoVerseMotion
import com.autoverse.launcher.core.launcher.LaunchOutcome
import com.autoverse.launcher.core.launcher.launchFailureMessage
import com.autoverse.launcher.core.media.MediaCommand
import com.autoverse.launcher.core.media.MediaCommandOutcome
import com.autoverse.launcher.core.media.MediaPresentationMode
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.isPlaying
import com.autoverse.launcher.core.media.mediaCommandFailureMessage
import com.autoverse.launcher.core.media.mediaPresentation
import com.autoverse.launcher.core.media.mediaToggleCommand
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.navigation.navigationFailureMessage
import com.autoverse.launcher.core.performance.LocalPerformanceSpec
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DashboardData(
    val clockText: String,
    val dateText: String,
    val fpsText: String,
    val connectivity: ConnectivityStatus = ConnectivityStatus(),
)

/**
 * Media wiring bundled for the dashboard layouts: the current state plus the
 * callbacks the media surfaces need. Defaults render the no-session surface,
 * so previews and tests work without a media layer.
 */
internal data class MediaSection(
    val state: MediaUiState = MediaUiState.NoSession,
    val loadArtwork: suspend (MediaUiState) -> ImageBitmap? = { null },
    val onCommand: (MediaCommand) -> Unit = {},
    val onOpenAccess: () -> Unit = {},
    val onBrowseApps: () -> Unit = {},
    val onRefresh: () -> Unit = {},
)

/**
 * Navigation wiring bundled for the dashboard layouts: the effective provider
 * plus the callbacks the navigation surfaces need. Defaults to the system
 * default provider, so previews and tests work without a navigation layer.
 */
internal data class NavigationSection(
    val resolved: ResolvedNavigationProvider = ResolvedNavigationProvider(
        provider = NavigationProvider.SystemDefault,
        preferredMissing = false,
    ),
    val onHandoff: (NavigationDestination) -> Unit = {},
    val onOpenApp: () -> Unit = {},
    val onChangeProvider: () -> Unit = {},
)

@Composable
fun HomeScreen(
    data: DashboardData,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteApps: List<InstalledApp> = emptyList(),
    onLaunchApp: suspend (InstalledApp) -> LaunchOutcome = { LaunchOutcome.LAUNCHED },
    onOpenAppDrawer: () -> Unit = {},
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap? = { null },
    mediaState: MediaUiState = MediaUiState.NoSession,
    onMediaCommand: suspend (MediaCommand) -> MediaCommandOutcome = { MediaCommandOutcome.SENT },
    onOpenMediaAccessSettings: () -> Boolean = { false },
    loadMediaArtwork: suspend (MediaUiState) -> ImageBitmap? = { null },
    onRefreshMedia: () -> Unit = {},
    navigationProvider: ResolvedNavigationProvider = ResolvedNavigationProvider(
        provider = NavigationProvider.SystemDefault,
        preferredMissing = false,
    ),
    onNavigateToDestination: suspend (NavigationDestination) -> NavigationOutcome = { NavigationOutcome.OPENED },
    onOpenNavigationApp: suspend () -> NavigationOutcome = { NavigationOutcome.OPENED },
    onOpenNavigation: () -> Unit = {},
    selectedProfile: PerformanceProfile = PerformanceProfile.BALANCED,
    onSelectProfile: (PerformanceProfile) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val onLaunchWithFeedback: (InstalledApp) -> Unit = { app ->
        scope.launch {
            val outcome = onLaunchApp(app)
            if (outcome != LaunchOutcome.LAUNCHED) {
                snackbarHostState.showSnackbar(
                    message = launchFailureMessage(app.label, outcome),
                    withDismissAction = true,
                )
            }
        }
    }

    val onMediaCommandWithFeedback: (MediaCommand) -> Unit = { command ->
        scope.launch {
            val outcome = onMediaCommand(command)
            if (outcome != MediaCommandOutcome.SENT) {
                snackbarHostState.showSnackbar(
                    message = mediaCommandFailureMessage(outcome),
                    withDismissAction = true,
                )
            }
        }
    }

    val onMediaAccess: () -> Unit = {
        if (!onOpenMediaAccessSettings()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Couldn't open system settings",
                    withDismissAction = true,
                )
            }
        }
    }

    val media = MediaSection(
        state = mediaState,
        loadArtwork = loadMediaArtwork,
        onCommand = onMediaCommandWithFeedback,
        onOpenAccess = onMediaAccess,
        onBrowseApps = onOpenAppDrawer,
        onRefresh = onRefreshMedia,
    )

    val providerLabel = navigationProvider.provider.label

    val onNavigationHandoff: (NavigationDestination) -> Unit = { destination ->
        scope.launch {
            val outcome = onNavigateToDestination(destination)
            if (outcome != NavigationOutcome.OPENED) {
                snackbarHostState.showSnackbar(
                    message = navigationFailureMessage(providerLabel, outcome),
                    withDismissAction = true,
                )
            }
        }
    }

    val onNavigationOpenApp: () -> Unit = {
        scope.launch {
            val outcome = onOpenNavigationApp()
            if (outcome != NavigationOutcome.OPENED) {
                snackbarHostState.showSnackbar(
                    message = navigationFailureMessage(providerLabel, outcome),
                    withDismissAction = true,
                )
            }
        }
    }

    val navigation = NavigationSection(
        resolved = navigationProvider,
        onHandoff = onNavigationHandoff,
        onOpenApp = onNavigationOpenApp,
        onChangeProvider = onOpenSettings,
    )

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val insets = WindowInsets.safeDrawing
            val insetWidthDp = with(density) {
                (insets.getLeft(this, layoutDirection) + insets.getRight(this, layoutDirection)).toDp()
            }
            val insetHeightDp = with(density) {
                (insets.getTop(this) + insets.getBottom(this)).toDp()
            }
            val policy = homeLayoutPolicyFor(
                widthDp = (maxWidth - insetWidthDp).value.roundToInt(),
                heightDp = (maxHeight - insetHeightDp).value.roundToInt(),
                fontScale = density.fontScale,
            )
            HomeScreenContent(
                data = data,
                policy = policy,
                onOpenSettings = onOpenSettings,
                onOpenDiagnostics = onOpenDiagnostics,
                favoriteApps = favoriteApps,
                onLaunchApp = onLaunchWithFeedback,
                onOpenAppDrawer = onOpenAppDrawer,
                onOpenNavigation = onOpenNavigation,
                loadAppIcon = loadAppIcon,
                media = media,
                navigation = navigation,
                selectedProfile = selectedProfile,
                onSelectProfile = onSelectProfile,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Stateless dashboard composition, split out from [HomeScreen] so previews
 * can render any layout archetype without a Scaffold or activity context.
 * The policy is computed by the caller from the available window space.
 * [contentInsets] is applied to the content column; the settings live preview
 * passes zero insets so the miniature dashboard is not padded a second time.
 */
@Composable
internal fun HomeScreenContent(
    data: DashboardData,
    policy: HomeLayoutPolicy,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    contentInsets: WindowInsets = WindowInsets.safeDrawing,
    favoriteApps: List<InstalledApp> = emptyList(),
    onLaunchApp: (InstalledApp) -> Unit = {},
    onOpenAppDrawer: () -> Unit = {},
    onOpenNavigation: () -> Unit = {},
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap? = { null },
    media: MediaSection = MediaSection(),
    navigation: NavigationSection = NavigationSection(),
    selectedProfile: PerformanceProfile = PerformanceProfile.BALANCED,
    onSelectProfile: (PerformanceProfile) -> Unit = {},
) {
    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current
    Box(
        modifier = modifier.then(
            if (spec.flatSurfaces) {
                Modifier.background(colors.backgroundTop)
            } else {
                Modifier.background(
                    Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)),
                )
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(contentInsets)
                .padding(
                    horizontal = AutoVerseSpacing.screenHorizontal,
                    vertical = AutoVerseSpacing.screenVertical,
                ),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
        ) {
            DashboardTopBar(
                clockText = data.clockText,
                dateText = data.dateText,
                connectivity = data.connectivity,
                chips = minOf(policy.statusChips, spec.maxStatusChips),
                fpsText = data.fpsText,
                showLogo = policy.archetype != HomeArchetype.RAIL_SPLIT,
                onOpenDiagnostics = onOpenDiagnostics,
                onOpenSettings = onOpenSettings,
            )
            when (policy.archetype) {
                HomeArchetype.RAIL_SPLIT -> RailSplitLayout(
                    policy = policy,
                    navigation = navigation,
                    favoriteApps = favoriteApps,
                    onLaunchApp = onLaunchApp,
                    onOpenAppDrawer = onOpenAppDrawer,
                    onOpenNavigation = onOpenNavigation,
                    loadAppIcon = loadAppIcon,
                    media = media,
                    selectedProfile = selectedProfile,
                    onSelectProfile = onSelectProfile,
                    onOpenSettings = onOpenSettings,
                    onOpenDiagnostics = onOpenDiagnostics,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                HomeArchetype.SPLIT_WITH_ACTIONS -> SplitWithActionsLayout(
                    policy = policy,
                    navigation = navigation,
                    favoriteApps = favoriteApps,
                    onLaunchApp = onLaunchApp,
                    onOpenAppDrawer = onOpenAppDrawer,
                    loadAppIcon = loadAppIcon,
                    media = media,
                    selectedProfile = selectedProfile,
                    onSelectProfile = onSelectProfile,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                HomeArchetype.COMPACT_SCROLL -> CompactScrollLayout(
                    policy = policy,
                    navigation = navigation,
                    favoriteApps = favoriteApps,
                    onLaunchApp = onLaunchApp,
                    onOpenAppDrawer = onOpenAppDrawer,
                    loadAppIcon = loadAppIcon,
                    media = media,
                    selectedProfile = selectedProfile,
                    onSelectProfile = onSelectProfile,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                HomeArchetype.MINIMAL -> MinimalLayout(
                    navigation = navigation,
                    favoriteApps = favoriteApps,
                    onLaunchApp = onLaunchApp,
                    onOpenAppDrawer = onOpenAppDrawer,
                    loadAppIcon = loadAppIcon,
                    media = media,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun RailSplitLayout(
    policy: HomeLayoutPolicy,
    navigation: NavigationSection,
    favoriteApps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenNavigation: () -> Unit,
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
    media: MediaSection,
    selectedProfile: PerformanceProfile,
    onSelectProfile: (PerformanceProfile) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
    ) {
        AppNavigationRail(
            selected = RailDestination.HOME,
            onOpenHome = {},
            onOpenAppDrawer = onOpenAppDrawer,
            onOpenNavigation = onOpenNavigation,
            onOpenDiagnostics = onOpenDiagnostics,
            onOpenSettings = onOpenSettings,
            modifier = Modifier
                .width(AutoVerseSpacing.railWidth)
                .fillMaxHeight(),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                NavigationCard(
                    resolved = navigation.resolved,
                    dense = policy.navigationDensity == NavigationDensity.COMPACT,
                    onHandoff = navigation.onHandoff,
                    onOpenApp = navigation.onOpenApp,
                    onChangeProvider = navigation.onChangeProvider,
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                )
                MediaWidget(
                    media = media,
                    dense = policy.navigationDensity == NavigationDensity.COMPACT,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap)) {
                QuickActionsRow(
                    favoriteApps = favoriteApps,
                    onLaunchApp = onLaunchApp,
                    onOpenAppDrawer = onOpenAppDrawer,
                    loadAppIcon = loadAppIcon,
                    modifier = Modifier.weight(1f),
                )
                DriveModeCard(
                    selected = selectedProfile,
                    onSelect = onSelectProfile,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SplitWithActionsLayout(
    policy: HomeLayoutPolicy,
    navigation: NavigationSection,
    favoriteApps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
    media: MediaSection,
    selectedProfile: PerformanceProfile,
    onSelectProfile: (PerformanceProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
        ) {
            NavigationCard(
                resolved = navigation.resolved,
                dense = policy.navigationDensity == NavigationDensity.COMPACT,
                onHandoff = navigation.onHandoff,
                onOpenApp = navigation.onOpenApp,
                onChangeProvider = navigation.onChangeProvider,
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxHeight(),
            )
            MediaWidget(
                media = media,
                dense = policy.navigationDensity == NavigationDensity.COMPACT,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap)) {
            QuickActionsRow(
                favoriteApps = favoriteApps,
                onLaunchApp = onLaunchApp,
                onOpenAppDrawer = onOpenAppDrawer,
                loadAppIcon = loadAppIcon,
                modifier = Modifier.weight(1f),
            )
            DriveModeCard(
                selected = selectedProfile,
                onSelect = onSelectProfile,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CompactScrollLayout(
    policy: HomeLayoutPolicy,
    navigation: NavigationSection,
    favoriteApps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
    media: MediaSection,
    selectedProfile: PerformanceProfile,
    onSelectProfile: (PerformanceProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
    ) {
        NavigationCard(
            resolved = navigation.resolved,
            dense = policy.navigationDensity == NavigationDensity.COMPACT,
            onHandoff = navigation.onHandoff,
            onOpenApp = navigation.onOpenApp,
            onChangeProvider = navigation.onChangeProvider,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (policy.navigationDensity == NavigationDensity.FULL) {
                        Modifier.heightIn(min = 280.dp)
                    } else {
                        Modifier
                    },
                ),
        )
        MediaWidget(
            media = media,
            dense = policy.navigationDensity == NavigationDensity.COMPACT,
            modifier = Modifier.fillMaxWidth(),
        )
        QuickActionsRow(
            favoriteApps = favoriteApps,
            onLaunchApp = onLaunchApp,
            onOpenAppDrawer = onOpenAppDrawer,
            loadAppIcon = loadAppIcon,
        )
        DriveModeCard(
            selected = selectedProfile,
            onSelect = onSelectProfile,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Very constrained fallback (short strips): clock stays in the status strip,
 * media collapses to a single row, navigation collapses to a single-row
 * destination bar, shortcuts keep the dock row. Scrollable, so no height
 * can clip it.
 */
@Composable
private fun MinimalLayout(
    navigation: NavigationSection,
    favoriteApps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
    media: MediaSection,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
    ) {
        CompactMediaRow(media)
        CompactNavigationRow(
            resolved = navigation.resolved,
            onHandoff = navigation.onHandoff,
            onOpenApp = navigation.onOpenApp,
            modifier = Modifier.fillMaxWidth(),
        )
        QuickActionsRow(
            favoriteApps = favoriteApps,
            onLaunchApp = onLaunchApp,
            onOpenAppDrawer = onOpenAppDrawer,
            loadAppIcon = loadAppIcon,
        )
    }
}

@Composable
private fun MediaWidget(
    media: MediaSection,
    dense: Boolean,
    modifier: Modifier = Modifier,
) {
    val presentation = remember(media.state) { mediaPresentation(media.state) }
    val artwork = rememberMediaArtwork(state = media.state, loadArtwork = media.loadArtwork)
    MediaCard(
        presentation = presentation,
        modifier = modifier,
        artwork = artwork,
        dense = dense,
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

/** Single-row media surface for the minimal archetype. */
@Composable
private fun CompactMediaRow(media: MediaSection) {
    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current
    val motion = LocalAutoVerseMotion.current
    val presentation = remember(media.state) { mediaPresentation(media.state) }
    val artwork = rememberMediaArtwork(state = media.state, loadArtwork = media.loadArtwork)
    val playing = presentation.status?.isPlaying == true
    CockpitCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
        ) {
            if (presentation.mode == MediaPresentationMode.ACTIVE) {
                Box(
                    modifier = Modifier
                        .size(AutoVerseIcons.prominent)
                        .clip(AutoVerseShapes.small)
                        .background(colors.surfaceHighlight),
                    contentAlignment = Alignment.Center,
                ) {
                    if (spec.artworkCrossfade) {
                        Crossfade(
                            targetState = artwork,
                            animationSpec = tween(motion.standardMs),
                            label = "compactArtwork",
                        ) { image ->
                            CompactArtwork(image = image, colors = colors)
                        }
                    } else {
                        CompactArtwork(image = artwork, colors = colors)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(AutoVerseIcons.dot)
                        .clip(CircleShape)
                        .background(colors.textFaint),
                )
            }
            Text(
                text = presentation.headline,
                style = MaterialTheme.typography.titleMedium,
                color = if (presentation.mode == MediaPresentationMode.ACTIVE) {
                    colors.textPrimary
                } else {
                    colors.textSecondary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            when (presentation.mode) {
                MediaPresentationMode.ACTIVE -> TransportButton(
                    icon = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    onClick = { media.onCommand(mediaToggleCommand(presentation.status)) },
                    variant = TransportVariant.SECONDARY,
                    enabled = presentation.controls.canTogglePlayPause,
                )

                MediaPresentationMode.ACCESS_REQUIRED -> TransportButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Open media access settings",
                    onClick = media.onOpenAccess,
                    variant = TransportVariant.SECONDARY,
                )

                else -> Unit
            }
        }
    }
}

/** Artwork or placeholder fill for the compact media row's artwork box. */
@Composable
private fun CompactArtwork(image: ImageBitmap?, colors: AutoVerseColors) {
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(
            Icons.Outlined.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.dot),
            tint = colors.textFaint,
        )
    }
}

/** Favorites dock: up to three favorite apps plus a permanent Apps tile. */
private const val MAX_DOCK_FAVORITES = 3

@Composable
private fun QuickActionsRow(
    favoriteApps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenAppDrawer: () -> Unit,
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
    ) {
        favoriteApps.take(MAX_DOCK_FAVORITES).forEach { app ->
            val icon by produceState<ImageBitmap?>(initialValue = null, app.key) {
                value = loadAppIcon(app)
            }
            AppDockItem(
                label = app.label,
                image = icon,
                onClick = { onLaunchApp(app) },
                modifier = Modifier.weight(1f),
            )
        }
        AppDockItem(
            icon = Icons.Outlined.Apps,
            label = "Apps",
            onClick = onOpenAppDrawer,
            modifier = Modifier.weight(1f),
        )
    }
}
