package com.autoverse.launcher.feature.diagnostics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.BuildConfig
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.DiagnosticsPaneMode
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.StatusChipsMode
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.AutoVerseScreen
import com.autoverse.launcher.core.design.components.DashboardTopBar
import com.autoverse.launcher.core.design.components.KeyValueRow
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.StatusChip
import com.autoverse.launcher.core.design.components.StatusTone
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.diagnosticsPanePolicyFor
import com.autoverse.launcher.core.design.isDarkTheme
import com.autoverse.launcher.core.design.tokens.AutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.design.tokens.effectiveReducedMotion
import com.autoverse.launcher.core.diagnostics.AccessItem
import com.autoverse.launcher.core.diagnostics.AppHealth
import com.autoverse.launcher.core.diagnostics.ComponentHealth
import com.autoverse.launcher.core.diagnostics.ComponentStatus
import com.autoverse.launcher.core.diagnostics.DiagnosticEvent
import com.autoverse.launcher.core.diagnostics.DiagnosticLevel
import com.autoverse.launcher.core.diagnostics.FrameSample
import com.autoverse.launcher.core.diagnostics.PermissionsSummary
import com.autoverse.launcher.core.diagnostics.SystemSnapshot
import com.autoverse.launcher.core.diagnostics.appDrawerHealth
import com.autoverse.launcher.core.diagnostics.appHealthFor
import com.autoverse.launcher.core.diagnostics.frameTrackingHealth
import com.autoverse.launcher.core.diagnostics.mediaSessionHealth
import com.autoverse.launcher.core.diagnostics.navigationHandoffHealth
import com.autoverse.launcher.core.diagnostics.settingsStoreHealth
import com.autoverse.launcher.core.diagnostics.summarizePermissions
import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.navigation.supportsOpenApp
import com.autoverse.launcher.core.performance.DeviceProfile
import com.autoverse.launcher.core.performance.FrameTimeTracker
import com.autoverse.launcher.core.performance.LocalPerformanceSpec
import com.autoverse.launcher.core.util.formatLogTimestamp
import kotlin.math.roundToInt

/** How many log events the Recent logs panel previews before "View all". */
private const val LOG_PREVIEW_COUNT = 6

/** Fixed plot height for the frame-history sparkline. */
private val CHART_HEIGHT = 120.dp

/**
 * Phase 17 — the full diagnostics surface. Everything shown here comes from
 * state the rest of the app already observes: the frame tracker, the system
 * sampler (only running while this screen is visible), the app catalog, the
 * media session state, the settings store, and the bounded in-memory log.
 * Nothing on this screen is fabricated, and unavailable readings say so.
 *
 * Layout follows [diagnosticsPanePolicyFor]: WIDE keeps the navigation rail
 * and splits the information into two columns; MEDIUM and COMPACT collapse to
 * a single scrolling column with a back affordance in the top bar.
 */
@Composable
fun DiagnosticsScreen(
    clockText: String,
    dateText: String,
    connectivity: ConnectivityStatus,
    profile: DeviceProfile,
    selectedProfile: PerformanceProfile,
    frameSnapshot: FrameTimeTracker.Snapshot,
    frameHistory: List<FrameSample>,
    frameTrackingActive: Boolean,
    systemSnapshot: SystemSnapshot?,
    diagnosticsEvents: List<DiagnosticEvent>,
    appListState: AppListState,
    mediaState: MediaUiState,
    navigationProvider: ResolvedNavigationProvider,
    settingsStoreReadable: Boolean?,
    accessItems: List<AccessItem>,
    notificationAccessGranted: Boolean,
    settings: AppSettings,
    systemAnimationsDisabled: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onClearLog: () -> Unit,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onOpenPerformanceSettings: () -> Unit,
    onOpenPermissionsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current
    var showAllLogs by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val insets = WindowInsets.safeDrawing
        val insetWidthDp = with(density) {
            (insets.getLeft(this, layoutDirection) + insets.getRight(this, layoutDirection)).toDp()
        }
        val insetHeightDp = with(density) {
            (insets.getTop(this) + insets.getBottom(this)).toDp()
        }
        val mode = diagnosticsPanePolicyFor(
            widthDp = (maxWidth - insetWidthDp).value.roundToInt(),
            heightDp = (maxHeight - insetHeightDp).value.roundToInt(),
            fontScale = density.fontScale,
        )
        val appHealth = appHealthFor(settingsStoreReadable, appListState)
        val reducedMotion = effectiveReducedMotion(
            userSetting = settings.reducedMotion,
            systemAnimationsDisabled = systemAnimationsDisabled,
        )
        val effectiveDark = isDarkTheme(settings.appearanceMode, isSystemInDarkTheme())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (spec.flatSurfaces) {
                        Modifier.background(colors.backgroundTop)
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                listOf(colors.backgroundTop, colors.backgroundBottom),
                            ),
                        )
                    },
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(
                        horizontal = AutoVerseSpacing.screenHorizontal,
                        vertical = AutoVerseSpacing.screenVertical,
                    ),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
            ) {
                when (mode) {
                    DiagnosticsPaneMode.WIDE -> Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                    ) {
                        AppNavigationRail(
                            selected = RailDestination.DIAGNOSTICS,
                            onOpenHome = onOpenHome,
                        onOpenAppDrawer = onOpenAppDrawer,
                        onOpenNavigation = onOpenNavigation,
                        onOpenDiagnostics = {},
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier
                                .width(AutoVerseSpacing.railWidth)
                                .fillMaxHeight(),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                        ) {
                            DiagnosticsTopBar(
                                mode = mode,
                                clockText = clockText,
                                dateText = dateText,
                                connectivity = connectivity,
                                onBack = onBack,
                                onOpenSettings = onOpenSettings,
                            )
                            RefreshRow(refreshing = refreshing, onRefresh = onRefresh)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                            ) {
                                SummarySection(
                                    mode = mode,
                                    health = appHealth,
                                    selectedProfile = selectedProfile,
                                    effectiveProfile = spec.profile,
                                    preferSmoothness = settings.preferSmoothness,
                                    reducedMotion = reducedMotion,
                                    themeId = settings.themeId,
                                    appearanceMode = settings.appearanceMode,
                                    effectiveDark = effectiveDark,
                                    accessItems = accessItems,
                                    onOpenAppearanceSettings = onOpenAppearanceSettings,
                                    onOpenPerformanceSettings = onOpenPerformanceSettings,
                                    onOpenPermissionsSettings = onOpenPermissionsSettings,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                                    ) {
                                        PerformancePanel(
                                            frameSnapshot = frameSnapshot,
                                            frameHistory = frameHistory,
                                            frameTrackingActive = frameTrackingActive,
                                            systemSnapshot = systemSnapshot,
                                        )
                                        SystemInfoPanel(profile = profile)
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                                    ) {
                                        ComponentsPanel(
                                            navigationProvider = navigationProvider,
                                            mediaState = mediaState,
                                            appListState = appListState,
                                            settingsStoreReadable = settingsStoreReadable,
                                            frameTrackingActive = frameTrackingActive,
                                            notificationAccessGranted = notificationAccessGranted,
                                        )
                                        LogsPanel(
                                            events = diagnosticsEvents,
                                            onViewAll = { showAllLogs = true },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    DiagnosticsPaneMode.MEDIUM, DiagnosticsPaneMode.COMPACT -> Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                    ) {
                        DiagnosticsTopBar(
                            mode = mode,
                            clockText = clockText,
                            dateText = dateText,
                            connectivity = connectivity,
                            onBack = onBack,
                            onOpenSettings = onOpenSettings,
                        )
                        RefreshRow(refreshing = refreshing, onRefresh = onRefresh)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                        ) {
                            SummarySection(
                                mode = mode,
                                health = appHealth,
                                selectedProfile = selectedProfile,
                                effectiveProfile = spec.profile,
                                preferSmoothness = settings.preferSmoothness,
                                reducedMotion = reducedMotion,
                                themeId = settings.themeId,
                                appearanceMode = settings.appearanceMode,
                                effectiveDark = effectiveDark,
                                accessItems = accessItems,
                                onOpenAppearanceSettings = onOpenAppearanceSettings,
                                onOpenPerformanceSettings = onOpenPerformanceSettings,
                                onOpenPermissionsSettings = onOpenPermissionsSettings,
                            )
                            PerformancePanel(
                                frameSnapshot = frameSnapshot,
                                frameHistory = frameHistory,
                                frameTrackingActive = frameTrackingActive,
                                systemSnapshot = systemSnapshot,
                            )
                            SystemInfoPanel(profile = profile)
                            ComponentsPanel(
                                navigationProvider = navigationProvider,
                                mediaState = mediaState,
                                appListState = appListState,
                                settingsStoreReadable = settingsStoreReadable,
                                frameTrackingActive = frameTrackingActive,
                                notificationAccessGranted = notificationAccessGranted,
                            )
                            LogsPanel(
                                events = diagnosticsEvents,
                                onViewAll = { showAllLogs = true },
                            )
                        }
                    }
                }
            }
            if (showAllLogs) {
                LogDetail(
                    events = diagnosticsEvents,
                    onClear = onClearLog,
                    onClose = { showAllLogs = false },
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsTopBar(
    mode: DiagnosticsPaneMode,
    clockText: String,
    dateText: String,
    connectivity: ConnectivityStatus,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    DashboardTopBar(
        clockText = clockText,
        dateText = dateText,
        connectivity = connectivity,
        chips = StatusChipsMode.NONE,
        onBack = if (mode == DiagnosticsPaneMode.WIDE) null else onBack,
        onOpenDiagnostics = null,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
private fun RefreshRow(refreshing: Boolean, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        PrimaryAction(
            text = if (refreshing) "Refreshing…" else "Refresh All",
            onClick = onRefresh,
            enabled = !refreshing,
        )
    }
}

/**
 * The four summary cards. WIDE shows them as one row of four; otherwise they
 * wrap into a 2x2 grid. Rows use intrinsic height so every card in a row
 * matches regardless of copy length.
 */
@Composable
private fun SummarySection(
    mode: DiagnosticsPaneMode,
    health: AppHealth,
    selectedProfile: PerformanceProfile,
    effectiveProfile: PerformanceProfile,
    preferSmoothness: Boolean,
    reducedMotion: Boolean,
    themeId: AutoVerseThemeId,
    appearanceMode: AppearanceMode,
    effectiveDark: Boolean,
    accessItems: List<AccessItem>,
    onOpenAppearanceSettings: () -> Unit,
    onOpenPerformanceSettings: () -> Unit,
    onOpenPermissionsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status: @Composable (Modifier) -> Unit = { cardModifier ->
        AppStatusCard(health = health, modifier = cardModifier)
    }
    val performance: @Composable (Modifier) -> Unit = { cardModifier ->
        PerformanceCard(
            selected = selectedProfile,
            effective = effectiveProfile,
            preferSmoothness = preferSmoothness,
            reducedMotion = reducedMotion,
            onOpenPerformanceSettings = onOpenPerformanceSettings,
            modifier = cardModifier,
        )
    }
    val theme: @Composable (Modifier) -> Unit = { cardModifier ->
        ThemeCard(
            themeId = themeId,
            appearanceMode = appearanceMode,
            effectiveDark = effectiveDark,
            onOpenAppearanceSettings = onOpenAppearanceSettings,
            modifier = cardModifier,
        )
    }
    val permissions: @Composable (Modifier) -> Unit = { cardModifier ->
        PermissionsCard(
            items = accessItems,
            onOpenPermissionsSettings = onOpenPermissionsSettings,
            modifier = cardModifier,
        )
    }
    if (mode == DiagnosticsPaneMode.WIDE) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
        ) {
            status(Modifier.weight(1f).fillMaxHeight())
            performance(Modifier.weight(1f).fillMaxHeight())
            theme(Modifier.weight(1f).fillMaxHeight())
            permissions(Modifier.weight(1f).fillMaxHeight())
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
            ) {
                status(Modifier.weight(1f).fillMaxHeight())
                performance(Modifier.weight(1f).fillMaxHeight())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
            ) {
                theme(Modifier.weight(1f).fillMaxHeight())
                permissions(Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun AppStatusCard(health: AppHealth, modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "App status",
        icon = Icons.Outlined.CheckCircle,
        modifier = modifier,
    ) {
        Text(
            text = appStatusLabel(health),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        StatusChip(
            label = "v${BuildConfig.VERSION_NAME}",
            tone = when (health) {
                AppHealth.NORMAL -> StatusTone.SUCCESS
                AppHealth.PARTIAL -> StatusTone.NEUTRAL
                AppHealth.DEGRADED -> StatusTone.DANGER
            },
            leadingDot = true,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        Text(
            text = appStatusDetail(health),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun PerformanceCard(
    selected: PerformanceProfile,
    effective: PerformanceProfile,
    preferSmoothness: Boolean,
    reducedMotion: Boolean,
    onOpenPerformanceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "Performance profile",
        icon = Icons.Outlined.Speed,
        modifier = modifier,
    ) {
        Text(
            text = profileSummaryText(effective),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        Text(
            text = profileNoteText(selected, effective, preferSmoothness, reducedMotion)
                ?: "Running with the stored profile.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        TextAction(text = "Change", onClick = onOpenPerformanceSettings)
    }
}

@Composable
private fun ThemeCard(
    themeId: AutoVerseThemeId,
    appearanceMode: AppearanceMode,
    effectiveDark: Boolean,
    onOpenAppearanceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "Theme",
        icon = Icons.Outlined.Palette,
        modifier = modifier,
    ) {
        Text(
            text = themeId.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        Text(
            text = appearanceDetailText(appearanceMode, effectiveDark),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        TextAction(text = "Change", onClick = onOpenAppearanceSettings)
    }
}

@Composable
private fun PermissionsCard(
    items: List<AccessItem>,
    onOpenPermissionsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val summary = summarizePermissions(items)
    WidgetContainer(
        title = "Permissions",
        icon = Icons.Outlined.Lock,
        modifier = modifier,
    ) {
        Text(
            text = permissionsSummaryLabel(summary),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        StatusChip(
            label = permissionsDetailText(items),
            tone = when (summary) {
                PermissionsSummary.ALL_GOOD -> StatusTone.SUCCESS
                PermissionsSummary.OPTIONAL_OFF -> StatusTone.NEUTRAL
                PermissionsSummary.ACTION_NEEDED -> StatusTone.WARNING
            },
            leadingDot = true,
        )
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        TextAction(text = "Review", onClick = onOpenPermissionsSettings)
    }
}

/**
 * Live performance readings plus the frame-history sparkline. Readings that
 * could not be sampled are labeled "Not readable" rather than shown as zero.
 */
@Composable
private fun PerformancePanel(
    frameSnapshot: FrameTimeTracker.Snapshot,
    frameHistory: List<FrameSample>,
    frameTrackingActive: Boolean,
    systemSnapshot: SystemSnapshot?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "Performance",
        subtitle = "Sampled about once a second while this screen is visible.",
        icon = Icons.Outlined.Speed,
        modifier = modifier,
    ) {
        KeyValueRow(
            label = "Frames per second",
            value = fpsReadingText(frameSnapshot.fps, frameTrackingActive),
        )
        KeyValueRow(label = "Frames sampled", value = frameSnapshot.frameCount.toString())
        KeyValueRow(label = "Janky frames (>33 ms)", value = frameSnapshot.jankCount.toString())
        KeyValueRow(
            label = "CPU (one core = 100%)",
            value = cpuReadingText(systemSnapshot?.processCpuPercentOfCore),
        )
        KeyValueRow(
            label = "App memory (PSS)",
            value = memoryReadingText(systemSnapshot?.processMemoryPssMb),
        )
        KeyValueRow(
            label = "Device memory",
            value = deviceMemoryReadingText(
                systemSnapshot?.deviceMemoryAvailableMb,
                systemSnapshot?.deviceMemoryTotalMb,
            ),
        )
        KeyValueRow(
            label = "Storage free",
            value = storageReadingText(systemSnapshot?.storageAvailableMb),
        )
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        FrameHistoryChart(
            samples = frameHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(CHART_HEIGHT),
        )
        val caption = historyCaption(frameHistory.size)
        if (caption != null) {
            Spacer(Modifier.size(AutoVerseSpacing.xxs))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textFaint,
            )
        }
    }
}

/**
 * Minimal sparkline over the latest frame samples (oldest left, newest
 * right), scaled to at least 60 FPS so the axis stays comparable over time.
 * Renders nothing until at least one sample exists.
 */
@Composable
private fun FrameHistoryChart(samples: List<FrameSample>, modifier: Modifier = Modifier) {
    if (samples.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "History builds while this screen samples.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAutoVerseColors.current.textFaint,
            )
        }
        return
    }
    val colors = LocalAutoVerseColors.current
    val scaleMax = maxOf(60f, samples.maxOf { it.fps }).coerceAtLeast(1f)
    Canvas(modifier = modifier) {
        val baseline = 1.dp.toPx()
        drawLine(
            color = colors.outline,
            start = Offset(0f, size.height - baseline / 2f),
            end = Offset(size.width, size.height - baseline / 2f),
            strokeWidth = baseline,
        )
        fun pointFor(index: Int): Offset {
            val x = if (samples.size == 1) {
                size.width
            } else {
                size.width * index / (samples.size - 1)
            }
            val y = size.height -
                (samples[index].fps / scaleMax).coerceIn(0f, 1f) * size.height
            return Offset(x, y)
        }
        if (samples.size == 1) {
            drawCircle(
                color = colors.accent,
                radius = 3.dp.toPx(),
                center = pointFor(0),
            )
        } else {
            val path = Path()
            samples.indices.forEach { index ->
                val point = pointFor(index)
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }
            drawPath(
                path = path,
                color = colors.accent,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

@Composable
private fun SystemInfoPanel(profile: DeviceProfile, modifier: Modifier = Modifier) {
    WidgetContainer(
        title = "System information",
        icon = Icons.Outlined.PhoneAndroid,
        modifier = modifier,
    ) {
        KeyValueRow(label = "Device", value = "${profile.manufacturer} ${profile.model}")
        KeyValueRow(
            label = "Android",
            value = "${profile.androidRelease} (API ${profile.sdkInt})",
        )
        KeyValueRow(label = "Total RAM", value = "${profile.totalRamMb} MB")
        KeyValueRow(label = "CPU cores", value = profile.cpuCores.toString())
        KeyValueRow(
            label = "Screen",
            value = "${profile.screenWidthDp} × ${profile.screenHeightDp} dp",
        )
        KeyValueRow(label = "Density", value = "${profile.densityDpi} dpi")
        KeyValueRow(label = "Low-RAM device", value = if (profile.lowRamDevice) "Yes" else "No")
        KeyValueRow(label = "Detected tier", value = profile.tier.displayName)
    }
}

/**
 * Component health tiles. Every tile maps already-observed state — the
 * builders run no checks of their own, so this panel can never disagree with
 * what the rest of the app is showing.
 */
@Composable
private fun ComponentsPanel(
    navigationProvider: ResolvedNavigationProvider,
    mediaState: MediaUiState,
    appListState: AppListState,
    settingsStoreReadable: Boolean?,
    frameTrackingActive: Boolean,
    notificationAccessGranted: Boolean,
    modifier: Modifier = Modifier,
) {
    val provider = navigationProvider.provider
    val components = listOf(
        navigationHandoffHealth(
            providerLabel = provider.label,
            preferredMissing = navigationProvider.preferredMissing,
            hasLaunchTarget = provider.supportsDestination || provider.supportsOpenApp,
        ),
        mediaSessionHealth(notificationAccessGranted, mediaState),
        appDrawerHealth(appListState),
        settingsStoreHealth(settingsStoreReadable),
        frameTrackingHealth(frameTrackingActive),
    )
    WidgetContainer(
        title = "App components",
        subtitle = "Live checks only; nothing is enabled or changed from here.",
        icon = Icons.Outlined.Extension,
        modifier = modifier,
    ) {
        components.forEach { component ->
            ComponentRow(component)
        }
    }
}

@Composable
private fun ComponentRow(health: ComponentHealth) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AutoVerseSpacing.xxs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = health.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            StatusChip(
                label = componentStatusLabel(health.status),
                tone = componentTone(health.status),
                leadingDot = true,
            )
        }
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        Text(
            text = health.detail,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

/** Recent-log preview; the full list opens as its own in-app detail view. */
@Composable
private fun LogsPanel(
    events: List<DiagnosticEvent>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WidgetContainer(
        title = "Recent logs",
        subtitle = "Bounded, in-memory only; nothing leaves the device.",
        icon = Icons.Outlined.History,
        modifier = modifier,
    ) {
        if (events.isEmpty()) {
            Text(
                text = "No events recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAutoVerseColors.current.textFaint,
            )
        } else {
            events.take(LOG_PREVIEW_COUNT).forEach { event ->
                LogRow(event)
            }
            TextAction(text = "View all", onClick = onViewAll)
        }
    }
}

@Composable
private fun LogRow(event: DiagnosticEvent, modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = AutoVerseSpacing.xxs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        Text(
            text = formatLogTimestamp(event.timeMillis),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textFaint,
        )
        Text(
            text = levelLabel(event.level),
            style = MaterialTheme.typography.labelMedium,
            color = levelColor(event.level, colors),
        )
        Text(
            text = event.message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Full log view, shown over the screen instead of navigating: it keeps the
 * Back handler scoped to closing this layer (the launcher's own Back handler
 * stays active underneath) and owns its scrolling so the list can be long.
 */
@Composable
private fun LogDetail(
    events: List<DiagnosticEvent>,
    onClear: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler { onClose() }
    AutoVerseScreen(
        title = "Diagnostic log",
        onBack = onClose,
        scrollable = false,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(
                label = "${events.size} events",
                tone = StatusTone.NEUTRAL,
                leadingDot = true,
            )
            Spacer(Modifier.weight(1f))
            TextAction(text = "Clear log", onClick = onClear, enabled = events.isNotEmpty())
        }
        if (events.isEmpty()) {
            Text(
                text = "No events recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAutoVerseColors.current.textFaint,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
            ) {
                items(items = events, key = { it.id }) { event ->
                    LogRow(event)
                }
            }
        }
    }
}

private fun historyCaption(count: Int): String? = when (count) {
    0 -> null
    1 -> "1 sample · 1-second windows · newest right"
    else -> "$count samples · 1-second windows · newest right"
}

private fun componentTone(status: ComponentStatus): StatusTone = when (status) {
    ComponentStatus.READY -> StatusTone.SUCCESS
    ComponentStatus.PERMISSION_REQUIRED -> StatusTone.WARNING
    ComponentStatus.UNAVAILABLE -> StatusTone.WARNING
    ComponentStatus.ERROR -> StatusTone.DANGER
    ComponentStatus.LOADING, ComponentStatus.DISABLED, ComponentStatus.NOT_CHECKED ->
        StatusTone.NEUTRAL
}

private fun levelLabel(level: DiagnosticLevel): String = when (level) {
    DiagnosticLevel.INFO -> "INFO"
    DiagnosticLevel.WARN -> "WARN"
    DiagnosticLevel.ERROR -> "ERROR"
}

private fun levelColor(level: DiagnosticLevel, colors: AutoVerseColors): Color = when (level) {
    DiagnosticLevel.INFO -> colors.textSecondary
    DiagnosticLevel.WARN -> colors.warning
    DiagnosticLevel.ERROR -> colors.danger
}
