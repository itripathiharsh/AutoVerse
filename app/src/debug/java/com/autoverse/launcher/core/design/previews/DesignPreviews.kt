package com.autoverse.launcher.core.design.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.connectivity.LinkState
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AppDockItem
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.CompactNavigationRow
import com.autoverse.launcher.core.design.components.KeyValueRow
import com.autoverse.launcher.core.design.components.MediaCard
import com.autoverse.launcher.core.design.components.NavigationCard
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.SelectRow
import com.autoverse.launcher.core.design.components.StatusChip
import com.autoverse.launcher.core.design.components.StatusTone
import com.autoverse.launcher.core.design.components.SwitchRow
import com.autoverse.launcher.core.design.components.TransportButton
import com.autoverse.launcher.core.design.components.TransportVariant
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.homeLayoutPolicyFor
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.diagnostics.DiagnosticEvent
import com.autoverse.launcher.core.diagnostics.DiagnosticLevel
import com.autoverse.launcher.core.diagnostics.FrameSample
import com.autoverse.launcher.core.diagnostics.SystemSnapshot
import com.autoverse.launcher.core.diagnostics.buildAccessReport
import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.launcher.LaunchOutcome
import com.autoverse.launcher.core.media.MediaAction
import com.autoverse.launcher.core.media.MediaProgress
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.PlaybackStatus
import com.autoverse.launcher.core.media.mediaControlsOf
import com.autoverse.launcher.core.media.mediaPresentation
import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.navigation.SavedPlace
import com.autoverse.launcher.core.performance.DeviceProfile
import com.autoverse.launcher.core.performance.FrameTimeTracker
import com.autoverse.launcher.core.performance.PerformanceTier
import com.autoverse.launcher.feature.apps.AppDrawerScreen
import com.autoverse.launcher.feature.apps.AppDrawerView
import com.autoverse.launcher.feature.diagnostics.DiagnosticsScreen
import com.autoverse.launcher.feature.home.DashboardData
import com.autoverse.launcher.feature.home.HomeScreenContent
import com.autoverse.launcher.feature.home.MediaSection
import com.autoverse.launcher.feature.home.NavigationSection
import com.autoverse.launcher.feature.navigation.NavigationScreen
import com.autoverse.launcher.feature.settings.SettingsScreen
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Palette boards
// ---------------------------------------------------------------------------

@Preview(name = "Palette — Obsidian Night", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteNightPreview() {
    PalettePreview(AutoVerseThemeId.OBSIDIAN, AppearanceMode.NIGHT)
}

@Preview(name = "Palette — Obsidian Day", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteDayPreview() {
    PalettePreview(AutoVerseThemeId.OBSIDIAN, AppearanceMode.DAY)
}

@Preview(name = "Palette — Neon Pulse Night", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteNeonPulseNightPreview() {
    PalettePreview(AutoVerseThemeId.NEON_PULSE, AppearanceMode.NIGHT)
}

@Preview(name = "Palette — Neon Pulse Day", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteNeonPulseDayPreview() {
    PalettePreview(AutoVerseThemeId.NEON_PULSE, AppearanceMode.DAY)
}

@Preview(name = "Palette — Atelier Night", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteAtelierNightPreview() {
    PalettePreview(AutoVerseThemeId.ATELIER, AppearanceMode.NIGHT)
}

@Preview(name = "Palette — Atelier Day", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteAtelierDayPreview() {
    PalettePreview(AutoVerseThemeId.ATELIER, AppearanceMode.DAY)
}

@Preview(name = "Palette — Apex Night", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteApexNightPreview() {
    PalettePreview(AutoVerseThemeId.APEX, AppearanceMode.NIGHT)
}

@Preview(name = "Palette — Apex Day", widthDp = 920, heightDp = 620)
@Composable
private fun PaletteApexDayPreview() {
    PalettePreview(AutoVerseThemeId.APEX, AppearanceMode.DAY)
}

@Composable
private fun PalettePreview(themeId: AutoVerseThemeId, appearanceMode: AppearanceMode) {
    AutoVerseTheme(themeId = themeId, appearanceMode = appearanceMode) {
        PaletteBoard()
    }
}

@Composable
private fun PaletteBoard() {
    val colors = LocalAutoVerseColors.current
    val swatches = listOf(
        "backgroundTop" to colors.backgroundTop,
        "backgroundBottom" to colors.backgroundBottom,
        "surface" to colors.surface,
        "surfaceRaised" to colors.surfaceRaised,
        "surfaceHighlight" to colors.surfaceHighlight,
        "outline" to colors.outline,
        "outlineStrong" to colors.outlineStrong,
        "accent" to colors.accent,
        "accentSoft" to colors.accentSoft,
        "onAccent" to colors.onAccent,
        "textPrimary" to colors.textPrimary,
        "textSecondary" to colors.textSecondary,
        "textFaint" to colors.textFaint,
        "disabledContainer" to colors.disabledContainer,
        "disabledContent" to colors.disabledContent,
        "success" to colors.success,
        "warning" to colors.warning,
        "danger" to colors.danger,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundTop)
            .padding(AutoVerseSpacing.xl)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
    ) {
        swatches.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md)) {
                rowItems.forEach { (name, value) ->
                    Swatch(name = name, value = value)
                }
            }
        }
    }
}

@Composable
private fun Swatch(name: String, value: Color) {
    val colors = LocalAutoVerseColors.current
    Column {
        Box(
            modifier = Modifier
                .size(width = 260.dp, height = 56.dp)
                .clip(AutoVerseShapes.small)
                .background(value)
                .border(AutoVerseBorders.hairline, colors.outline, AutoVerseShapes.small),
        )
        Spacer(Modifier.height(AutoVerseSpacing.xxs))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textFaint,
        )
    }
}

// ---------------------------------------------------------------------------
// Component gallery
// ---------------------------------------------------------------------------

@Preview(name = "Components — Night", widthDp = 920, heightDp = 1400)
@Composable
private fun ComponentGalleryNightPreview() {
    ComponentGalleryPreview(AutoVerseThemeId.OBSIDIAN, AppearanceMode.NIGHT)
}

@Preview(name = "Components — Day", widthDp = 920, heightDp = 1400)
@Composable
private fun ComponentGalleryDayPreview() {
    ComponentGalleryPreview(AutoVerseThemeId.OBSIDIAN, AppearanceMode.DAY)
}

@Preview(name = "Components — Neon Pulse", widthDp = 920, heightDp = 1400)
@Composable
private fun ComponentGalleryNeonPulsePreview() {
    ComponentGalleryPreview(AutoVerseThemeId.NEON_PULSE, AppearanceMode.NIGHT)
}

@Preview(name = "Components — Atelier", widthDp = 920, heightDp = 1400)
@Composable
private fun ComponentGalleryAtelierPreview() {
    ComponentGalleryPreview(AutoVerseThemeId.ATELIER, AppearanceMode.NIGHT)
}

@Preview(name = "Components — Apex", widthDp = 920, heightDp = 1400)
@Composable
private fun ComponentGalleryApexPreview() {
    ComponentGalleryPreview(AutoVerseThemeId.APEX, AppearanceMode.NIGHT)
}

@Composable
private fun ComponentGalleryPreview(themeId: AutoVerseThemeId, appearanceMode: AppearanceMode) {
    AutoVerseTheme(themeId = themeId, appearanceMode = appearanceMode) {
        ComponentGallery()
    }
}

@Composable
private fun ComponentGallery() {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.backgroundTop)
            .padding(AutoVerseSpacing.xl)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
    ) {
        WidgetContainer(title = "BUTTONS & ACTIONS", icon = Icons.Outlined.Speed) {
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md)) {
                PrimaryAction(text = "Set preferred app", onClick = {})
                PrimaryAction(text = "Disabled", onClick = {}, enabled = false)
            }
            Spacer(Modifier.height(AutoVerseSpacing.md))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
            ) {
                TransportButton(
                    icon = Icons.Outlined.Radio,
                    contentDescription = "Previous",
                    onClick = {},
                    variant = TransportVariant.SECONDARY,
                )
                TransportButton(
                    icon = Icons.Outlined.Cloud,
                    contentDescription = "Play",
                    onClick = {},
                    variant = TransportVariant.PRIMARY,
                )
                TransportButton(
                    icon = Icons.Outlined.Phone,
                    contentDescription = "Next",
                    onClick = {},
                    variant = TransportVariant.SECONDARY,
                )
                TransportButton(
                    icon = Icons.Outlined.Tune,
                    contentDescription = "Disabled",
                    onClick = {},
                    variant = TransportVariant.SECONDARY,
                    enabled = false,
                )
            }
        }

        WidgetContainer(title = "STATUS CHIPS", icon = Icons.Outlined.Tune) {
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
                StatusChip(icon = Icons.Outlined.DarkMode, label = "Theme")
                StatusChip(label = "60 FPS", leadingDot = true, tone = StatusTone.SUCCESS)
                StatusChip(label = "Warm", icon = Icons.Outlined.Warning, tone = StatusTone.WARNING)
                StatusChip(label = "Overheat", icon = Icons.Outlined.Warning, tone = StatusTone.DANGER)
            }
        }

        WidgetContainer(title = "DOCK ITEMS", icon = Icons.Outlined.Radio) {
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap)) {
                AppDockItem(
                    icon = Icons.Outlined.Radio,
                    label = "Radio",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                AppDockItem(
                    icon = Icons.Outlined.Phone,
                    label = "Phone",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                AppDockItem(
                    icon = Icons.Outlined.AcUnit,
                    label = "Climate",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
                AppDockItem(
                    icon = Icons.Outlined.Tune,
                    label = "Disabled",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    enabled = false,
                )
            }
        }

        WidgetContainer(title = "SETTINGS ROWS", icon = Icons.Outlined.Tune) {
            KeyValueRow(label = "Frames per second", value = "60")
            SelectRow(
                title = "Balanced",
                subtitle = "Smooth everyday driving",
                selected = true,
                enabled = true,
                onSelect = {},
            )
            SelectRow(
                title = "Enhanced",
                subtitle = "Coming in a later phase",
                selected = false,
                enabled = false,
                onSelect = {},
            )
            SwitchRow(
                title = "Reduce motion",
                subtitle = "Minimise transitions",
                checked = false,
                onCheckedChange = {},
            )
        }

        WidgetContainer(title = "CARD STATES", icon = Icons.Outlined.Cloud) {
            CockpitCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Static card — hairline border, layered gradient",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(AutoVerseSpacing.sm))
            CockpitCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
                Text(
                    text = "Tappable card — border brightens to accent while pressed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }

        WidgetContainer(title = "MEDIA CARD STATES", icon = Icons.Outlined.Cloud) {
            Column(verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
                MediaCard(
                    presentation = mediaPresentation(previewMediaActive),
                    modifier = Modifier.height(380.dp),
                )
                MediaCard(
                    presentation = mediaPresentation(MediaUiState.AccessRequired),
                    modifier = Modifier.height(380.dp),
                )
            }
        }

        WidgetContainer(title = "NAVIGATION CARD STATES", icon = Icons.Outlined.Speed) {
            Column(verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
                NavigationCard(
                    resolved = ResolvedNavigationProvider(
                        provider = NavigationProvider.GoogleMaps,
                        preferredMissing = false,
                    ),
                    modifier = Modifier.height(420.dp),
                )
                NavigationCard(
                    resolved = ResolvedNavigationProvider(
                        provider = NavigationProvider.Waze,
                        preferredMissing = false,
                    ),
                    dense = true,
                    modifier = Modifier.height(220.dp),
                )
                NavigationCard(
                    resolved = ResolvedNavigationProvider(
                        provider = NavigationProvider.SystemDefault,
                        preferredMissing = true,
                    ),
                    modifier = Modifier.height(420.dp),
                )
                NavigationCard(
                    resolved = ResolvedNavigationProvider(
                        provider = NavigationProvider.OpenOnly(
                            packageName = "com.example.nav",
                            label = "Sample Nav",
                        ),
                        preferredMissing = false,
                    ),
                    modifier = Modifier.height(340.dp),
                )
                CompactNavigationRow(
                    resolved = ResolvedNavigationProvider(
                        provider = NavigationProvider.GoogleMaps,
                        preferredMissing = false,
                    ),
                )
                CompactNavigationRow(
                    resolved = ResolvedNavigationProvider(
                        provider = NavigationProvider.SystemDefault,
                        preferredMissing = true,
                    ),
                )
            }
        }

        WidgetContainer(title = "TYPE SPECIMEN", icon = Icons.Outlined.DarkMode) {
            Text(
                text = "10:35",
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "Headline — screen titles",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "Title — card and row emphasis",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Text(
                text = "Body — supporting copy stays readable at arm's length.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Text(
                text = "LABEL — SECTION AND CHIP TEXT",
                style = MaterialTheme.typography.labelLarge,
                color = colors.accent,
            )
            Spacer(Modifier.height(AutoVerseSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
                IconSizes()
            }
        }
    }
}

@Composable
private fun IconSizes() {
    val colors = LocalAutoVerseColors.current
    val sizes = listOf(
        "16" to AutoVerseIcons.inline,
        "20" to AutoVerseIcons.compact,
        "24" to AutoVerseIcons.standard,
        "32" to AutoVerseIcons.prominent,
        "40" to AutoVerseIcons.hero,
    )
    sizes.forEach { (label, size) ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Speed,
                contentDescription = null,
                modifier = Modifier.size(size),
                tint = colors.accent,
            )
            Spacer(Modifier.height(AutoVerseSpacing.xxs))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
        }
    }
}

// ---------------------------------------------------------------------------
// Home dashboard — every layout archetype in both Obsidian appearances
// ---------------------------------------------------------------------------

private val previewDashboardData = DashboardData(
    clockText = "10:35",
    dateText = "Sat, Oct 17",
    fpsText = "60 FPS",
    connectivity = ConnectivityStatus(
        wifi = LinkState.CONNECTED,
        bluetooth = LinkState.CONNECTED,
    ),
)

/** Clearly fictional sample apps — never presented as real device data. */
private val previewSampleApps = listOf(
    InstalledApp(
        packageName = "com.example.maps",
        activityName = "com.example.maps.MainActivity",
        label = "Sample Maps",
        category = AppCategory.NAVIGATION,
        firstInstallTime = 1_700_000_000_000L,
    ),
    InstalledApp(
        packageName = "com.example.music",
        activityName = "com.example.music.PlayerActivity",
        label = "Sample Music",
        category = AppCategory.MUSIC_AUDIO,
        firstInstallTime = 1_700_100_000_000L,
    ),
    InstalledApp(
        packageName = "com.example.podcasts",
        activityName = "com.example.podcasts.MainActivity",
        label = "Sample Podcasts",
        category = AppCategory.MUSIC_AUDIO,
        firstInstallTime = 1_700_200_000_000L,
    ),
    InstalledApp(
        packageName = "com.example.notes",
        activityName = "com.example.notes.MainActivity",
        label = "Sample Notes",
        category = AppCategory.TOOLS,
        firstInstallTime = 1_700_300_000_000L,
    ),
)

/** Clearly fictional sample media — never presented as real device data. */
private val previewMediaActive = MediaUiState.Active(
    packageName = "com.example.music",
    appLabel = "Sample Music",
    title = "Sample Track No. 4",
    artist = "Sample Artist",
    album = "Sample Album",
    artworkUri = null,
    embeddedArtwork = null,
    status = PlaybackStatus.PLAYING,
    controls = mediaControlsOf(
        actions = setOf(
            MediaAction.PLAY,
            MediaAction.PAUSE,
            MediaAction.NEXT,
            MediaAction.PREVIOUS,
        ),
        status = PlaybackStatus.PLAYING,
    ),
    progress = MediaProgress(positionMs = 96_000L, durationMs = 240_000L),
)

@Composable
private fun HomePreview(
    widthDp: Int,
    heightDp: Int,
    appearanceMode: AppearanceMode,
    themeId: AutoVerseThemeId = AutoVerseThemeId.OBSIDIAN,
    mediaState: MediaUiState = MediaUiState.NoSession,
    navigation: NavigationSection = NavigationSection(),
) {
    AutoVerseTheme(themeId = themeId, appearanceMode = appearanceMode) {
        HomeScreenContent(
            data = previewDashboardData,
            policy = homeLayoutPolicyFor(widthDp = widthDp, heightDp = heightDp),
            onOpenSettings = {},
            onOpenDiagnostics = {},
            favoriteApps = previewSampleApps.take(3),
            media = MediaSection(state = mediaState),
            navigation = navigation,
        )
    }
}

@Preview(name = "Home — Compact (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun HomeCompactPreview() {
    HomePreview(widthDp = 411, heightDp = 891, appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Home — Compact, Day (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun HomeCompactDayPreview() {
    HomePreview(widthDp = 411, heightDp = 891, appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Home — Split (800x600)", widthDp = 800, heightDp = 600)
@Composable
private fun HomeSplitPreview() {
    HomePreview(widthDp = 800, heightDp = 600, appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Home — Split, Day (800x600)", widthDp = 800, heightDp = 600)
@Composable
private fun HomeSplitDayPreview() {
    HomePreview(widthDp = 800, heightDp = 600, appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Home — Split, short (800x400)", widthDp = 800, heightDp = 400)
@Composable
private fun HomeSplitShortPreview() {
    HomePreview(widthDp = 800, heightDp = 400, appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Home — Rail (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailPreview() {
    HomePreview(widthDp = 1280, heightDp = 800, appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Home — Rail, Day (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailDayPreview() {
    HomePreview(widthDp = 1280, heightDp = 800, appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Home — Rail, Cobalt (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailCobaltPreview() {
    HomePreview(
        widthDp = 1280,
        heightDp = 800,
        appearanceMode = AppearanceMode.NIGHT,
        themeId = AutoVerseThemeId.COBALT,
    )
}

@Preview(name = "Home — Rail, Neon Pulse (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailNeonPulsePreview() {
    HomePreview(
        widthDp = 1280,
        heightDp = 800,
        appearanceMode = AppearanceMode.NIGHT,
        themeId = AutoVerseThemeId.NEON_PULSE,
    )
}

@Preview(name = "Home — Rail, Atelier (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailAtelierPreview() {
    HomePreview(
        widthDp = 1280,
        heightDp = 800,
        appearanceMode = AppearanceMode.NIGHT,
        themeId = AutoVerseThemeId.ATELIER,
    )
}

@Preview(name = "Home — Rail, Apex (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailApexPreview() {
    HomePreview(
        widthDp = 1280,
        heightDp = 800,
        appearanceMode = AppearanceMode.NIGHT,
        themeId = AutoVerseThemeId.APEX,
    )
}

@Preview(name = "Home — Rail with active media (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailMediaPreview() {
    HomePreview(
        widthDp = 1280,
        heightDp = 800,
        appearanceMode = AppearanceMode.NIGHT,
        mediaState = previewMediaActive,
    )
}

@Preview(name = "Home — Minimal (800x220)", widthDp = 800, heightDp = 220)
@Composable
private fun HomeMinimalPreview() {
    HomePreview(widthDp = 800, heightDp = 220, appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Home — Minimal, Day (800x220)", widthDp = 800, heightDp = 220)
@Composable
private fun HomeMinimalDayPreview() {
    HomePreview(widthDp = 800, heightDp = 220, appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Home — Rail, Google Maps selected (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailGoogleMapsPreview() {
    HomePreview(
        widthDp = 1280,
        heightDp = 800,
        appearanceMode = AppearanceMode.NIGHT,
        navigation = NavigationSection(
            resolved = ResolvedNavigationProvider(
                provider = NavigationProvider.GoogleMaps,
                preferredMissing = false,
            ),
        ),
    )
}

@Preview(name = "Home — Rail, preferred nav app missing (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun HomeRailNavigationMissingPreview() {
    HomePreview(
        widthDp = 1280,
        heightDp = 800,
        appearanceMode = AppearanceMode.NIGHT,
        navigation = NavigationSection(
            resolved = ResolvedNavigationProvider(
                provider = NavigationProvider.SystemDefault,
                preferredMissing = true,
            ),
        ),
    )
}

// ---------------------------------------------------------------------------
// App drawer — ready, empty, loading, error states
// ---------------------------------------------------------------------------

@Composable
private fun DrawerPreview(
    state: AppListState,
    favorites: List<String> = emptyList(),
    appearanceMode: AppearanceMode = AppearanceMode.NIGHT,
    themeId: AutoVerseThemeId = AutoVerseThemeId.OBSIDIAN,
    initialView: AppDrawerView = AppDrawerView.GRID,
    initialCategory: AppCategory = AppCategory.ALL,
) {
    var viewMode by remember { mutableStateOf(initialView) }
    var category by remember { mutableStateOf(initialCategory) }
    var query by remember { mutableStateOf("") }
    AutoVerseTheme(themeId = themeId, appearanceMode = appearanceMode) {
        AppDrawerScreen(
            clockText = previewDashboardData.clockText,
            dateText = previewDashboardData.dateText,
            connectivity = previewDashboardData.connectivity,
            state = state,
            favorites = favorites,
            searchQuery = query,
            onSearchQueryChange = { query = it },
            viewMode = viewMode,
            onViewModeChange = { viewMode = it },
            category = category,
            onCategoryChange = { category = it },
            onToggleFavorite = {},
            onSetFavorites = {},
            onLaunchApp = { LaunchOutcome.LAUNCHED },
            onRefresh = {},
            loadIcon = { null },
            onBack = {},
            onOpenHome = {},
            onOpenSettings = {},
            onOpenDiagnostics = {},
            onOpenNavigation = {},
        )
    }
}

@Preview(name = "Apps — Drawer (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerRailPreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
    )
}

@Preview(name = "Apps — Drawer (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun AppDrawerCompactPreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
    )
}

@Preview(name = "Apps — Drawer, Day (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerRailDayPreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
        appearanceMode = AppearanceMode.DAY,
    )
}

@Preview(name = "Apps — Drawer Loading (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerLoadingPreview() {
    DrawerPreview(state = AppListState.Loading)
}

@Preview(name = "Apps — Drawer Empty (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerEmptyPreview() {
    DrawerPreview(state = AppListState.Ready(emptyList()))
}

@Preview(name = "Apps — Drawer Error (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerErrorPreview() {
    DrawerPreview(state = AppListState.Error("Package manager unavailable"))
}

@Preview(name = "Apps — Drawer, Neon Pulse (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerNeonPulsePreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
        themeId = AutoVerseThemeId.NEON_PULSE,
    )
}

@Preview(name = "Apps — Drawer, Atelier (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerAtelierPreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
        themeId = AutoVerseThemeId.ATELIER,
    )
}

@Preview(name = "Apps — Drawer, Apex (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerApexPreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
        themeId = AutoVerseThemeId.APEX,
    )
}

@Preview(name = "Apps — Drawer list view (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerListPreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
        initialView = AppDrawerView.LIST,
    )
}

@Preview(name = "Apps — Drawer, no favorites (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerNoFavoritesPreview() {
    DrawerPreview(state = AppListState.Ready(previewSampleApps))
}

@Preview(name = "Apps — Drawer, filtered empty (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun AppDrawerFilteredEmptyPreview() {
    DrawerPreview(
        state = AppListState.Ready(previewSampleApps),
        favorites = previewSampleApps.take(2).map { it.key },
        initialCategory = AppCategory.VIDEO,
    )
}

// ---------------------------------------------------------------------------
// Settings — wide, medium, compact; theme and appearance variants
// ---------------------------------------------------------------------------

/** Clearly fictional sample settings — never presented as real device state. */
private val previewSettings = AppSettings(
    themeId = AutoVerseThemeId.OBSIDIAN,
    appearanceMode = AppearanceMode.NIGHT,
    clock24h = true,
)

@Composable
private fun SettingsPreview(
    widthDp: Int,
    heightDp: Int,
    appearanceMode: AppearanceMode = AppearanceMode.NIGHT,
    themeId: AutoVerseThemeId = AutoVerseThemeId.OBSIDIAN,
) {
    AutoVerseTheme(themeId = themeId, appearanceMode = appearanceMode) {
        SettingsScreen(
            settings = previewSettings.copy(
                themeId = themeId,
                appearanceMode = appearanceMode,
            ),
            onSetTheme = {},
            onSetAppearanceMode = {},
            onSetReducedMotion = {},
            onSetTextScale = {},
            onSetProfile = {},
            onSetPreferSmoothness = {},
            onSetClock24h = {},
            onBack = {},
            notificationAccessGranted = true,
            onOpenNotificationAccessSettings = { true },
            favoriteApps = previewSampleApps.take(3),
            onLaunchApp = {},
            onRemoveFavorite = {},
            onOpenAppDrawer = {},
            loadAppIcon = { null },
            onOpenDiagnostics = {},
            onOpenHome = {},
            onOpenNavigation = {},
            clockText = previewDashboardData.clockText,
            dateText = previewDashboardData.dateText,
            connectivity = previewDashboardData.connectivity,
            dashboardPreview = { modifier ->
                BoxWithConstraints(modifier = modifier) {
                    HomeScreenContent(
                        data = previewDashboardData,
                        policy = homeLayoutPolicyFor(
                            widthDp = maxWidth.value.roundToInt(),
                            heightDp = maxHeight.value.roundToInt(),
                        ),
                        onOpenSettings = {},
                        onOpenDiagnostics = {},
                        contentInsets = WindowInsets(0, 0, 0, 0),
                        favoriteApps = previewSampleApps.take(3),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            onResetSettings = {},
        )
    }
}

@Preview(name = "Settings — Wide (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun SettingsWidePreview() {
    SettingsPreview(widthDp = 1280, heightDp = 800)
}

@Preview(name = "Settings — Wide, Day (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun SettingsWideDayPreview() {
    SettingsPreview(widthDp = 1280, heightDp = 800, appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Settings — Wide, Neon Pulse (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun SettingsWideNeonPulsePreview() {
    SettingsPreview(widthDp = 1280, heightDp = 800, themeId = AutoVerseThemeId.NEON_PULSE)
}

@Preview(name = "Settings — Medium (800x600)", widthDp = 800, heightDp = 600)
@Composable
private fun SettingsMediumPreview() {
    SettingsPreview(widthDp = 800, heightDp = 600)
}

@Preview(name = "Settings — Compact (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun SettingsCompactPreview() {
    SettingsPreview(widthDp = 411, heightDp = 891)
}

@Preview(name = "Settings — Compact, Day (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun SettingsCompactDayPreview() {
    SettingsPreview(widthDp = 411, heightDp = 891, appearanceMode = AppearanceMode.DAY)
}

// ---------------------------------------------------------------------------
// Diagnostics — wide, medium, compact; theme variants and honest empty states
// ---------------------------------------------------------------------------

/** Clearly fictional sample device — never presented as real hardware. */
private val previewDeviceProfile = DeviceProfile(
    manufacturer = "Sample Motors",
    model = "HU-9000",
    androidRelease = "14",
    sdkInt = 34,
    totalRamMb = 8_192,
    cpuCores = 8,
    lowRamDevice = false,
    densityDpi = 240,
    screenWidthDp = 1_280,
    screenHeightDp = 800,
    tier = PerformanceTier.ENHANCED,
)

private val previewFrameSnapshot = FrameTimeTracker.Snapshot(
    fps = 59.4f,
    frameCount = 2_140L,
    jankCount = 6L,
    sessionId = 1L,
    atMillis = 1_700_000_000_000L,
)

/** Deterministic sample curve — a plausible one-sample-per-second FPS trace. */
private val previewFrameHistory = listOf(
    58f, 59f, 57f, 60f, 55f, 52f, 58f, 59f, 60f, 58f,
    54f, 57f, 59f, 60f, 60f, 58f, 51f, 56f, 58f, 59f,
    60f, 57f, 55f, 58f, 60f, 59f, 58f, 57f, 59f, 60f,
).mapIndexed { index, fps ->
    FrameSample(fps = fps, atMillis = 1_700_000_000_000L + index * 1_000L)
}

/** Clearly fictional sample readings — never presented as real device state. */
private val previewSystemSnapshot = SystemSnapshot(
    processCpuPercentOfCore = 18.4f,
    processMemoryPssMb = 214,
    deviceMemoryAvailableMb = 2_300,
    deviceMemoryTotalMb = 8_192,
    storageAvailableMb = 21_450,
)

/** Clearly fictional sample events — never presented as real device state. */
private val previewDiagnosticEvents = listOf(
    DiagnosticEvent(
        id = 6L,
        timeMillis = 1_700_000_012_000L,
        level = DiagnosticLevel.INFO,
        message = "Refresh complete",
    ),
    DiagnosticEvent(
        id = 5L,
        timeMillis = 1_700_000_011_000L,
        level = DiagnosticLevel.INFO,
        message = "Manual refresh requested",
    ),
    DiagnosticEvent(
        id = 4L,
        timeMillis = 1_700_000_007_000L,
        level = DiagnosticLevel.WARN,
        message = "An app could not be opened",
    ),
    DiagnosticEvent(
        id = 3L,
        timeMillis = 1_700_000_004_000L,
        level = DiagnosticLevel.INFO,
        message = "App list loaded — 4 apps",
    ),
    DiagnosticEvent(
        id = 2L,
        timeMillis = 1_700_000_002_000L,
        level = DiagnosticLevel.INFO,
        message = "Settings loaded",
    ),
    DiagnosticEvent(
        id = 1L,
        timeMillis = 1_700_000_001_000L,
        level = DiagnosticLevel.INFO,
        message = "Launcher started",
    ),
)

private val previewAccessItems = buildAccessReport(
    sdkInt = 34,
    networkGranted = true,
    notificationAccessGranted = true,
)

private val previewResolvedNavigationProvider = ResolvedNavigationProvider(
    provider = NavigationProvider.SystemDefault,
    preferredMissing = false,
)

@Composable
private fun DiagnosticsPreview(
    appearanceMode: AppearanceMode = AppearanceMode.NIGHT,
    themeId: AutoVerseThemeId = AutoVerseThemeId.OBSIDIAN,
    events: List<DiagnosticEvent> = previewDiagnosticEvents,
    frameHistory: List<FrameSample> = previewFrameHistory,
    systemSnapshot: SystemSnapshot? = previewSystemSnapshot,
) {
    AutoVerseTheme(
        themeId = themeId,
        appearanceMode = appearanceMode,
    ) {
        DiagnosticsScreen(
            clockText = previewDashboardData.clockText,
            dateText = previewDashboardData.dateText,
            connectivity = previewDashboardData.connectivity,
            profile = previewDeviceProfile,
            selectedProfile = previewSettings.profile,
            frameSnapshot = previewFrameSnapshot,
            frameHistory = frameHistory,
            frameTrackingActive = true,
            systemSnapshot = systemSnapshot,
            diagnosticsEvents = events,
            appListState = AppListState.Ready(previewSampleApps),
            mediaState = previewMediaActive,
            navigationProvider = previewResolvedNavigationProvider,
            settingsStoreReadable = true,
            accessItems = previewAccessItems,
            notificationAccessGranted = true,
            settings = previewSettings.copy(
                themeId = themeId,
                appearanceMode = appearanceMode,
            ),
            systemAnimationsDisabled = false,
            refreshing = false,
            onRefresh = {},
            onClearLog = {},
            onBack = {},
            onOpenHome = {},
            onOpenAppDrawer = {},
            onOpenNavigation = {},
            onOpenSettings = {},
            onOpenAppearanceSettings = {},
            onOpenPerformanceSettings = {},
            onOpenPermissionsSettings = {},
        )
    }
}

@Preview(name = "Diagnostics — Wide (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun DiagnosticsWidePreview() {
    DiagnosticsPreview()
}

@Preview(name = "Diagnostics — Wide, Day (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun DiagnosticsWideDayPreview() {
    DiagnosticsPreview(appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Diagnostics — Wide, Neon Pulse (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun DiagnosticsWideNeonPulsePreview() {
    DiagnosticsPreview(themeId = AutoVerseThemeId.NEON_PULSE)
}

@Preview(name = "Diagnostics — Medium (800x600)", widthDp = 800, heightDp = 600)
@Composable
private fun DiagnosticsMediumPreview() {
    DiagnosticsPreview()
}

@Preview(name = "Diagnostics — Compact (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun DiagnosticsCompactPreview() {
    DiagnosticsPreview()
}

@Preview(name = "Diagnostics — Compact, Day (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun DiagnosticsCompactDayPreview() {
    DiagnosticsPreview(appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Diagnostics — Empty log (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun DiagnosticsEmptyLogPreview() {
    DiagnosticsPreview(events = emptyList())
}

@Preview(name = "Diagnostics — First run (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun DiagnosticsFirstRunPreview() {
    DiagnosticsPreview(
        events = emptyList(),
        frameHistory = emptyList(),
        systemSnapshot = null,
    )
}

// ---------------------------------------------------------------------------
// Phase 18 — Navigation page
// ---------------------------------------------------------------------------

/** Clearly fictional sample saved places — never presented as real user data. */
private val previewSavedPlaces = listOf(
    SavedPlace(
        label = "Sample Museum",
        destination = NavigationDestination.Query("Sample Museum, Vienna"),
    ),
    SavedPlace(
        label = "Sample Office",
        destination = NavigationDestination.Query("Sample Office Park"),
    ),
)

/** Clearly fictional sample handoffs, newest first. */
private val previewRecentDestinations = listOf(
    RecentDestination(destination = NavigationDestination.Query("Sample Airport")),
    RecentDestination(destination = NavigationDestination.Query("Sample Train Station")),
)

@Composable
private fun NavigationPagePreview(
    appearanceMode: AppearanceMode,
    themeId: AutoVerseThemeId = AutoVerseThemeId.OBSIDIAN,
    query: String = "sample",
    placesPanelVisible: Boolean = true,
    savedPlaces: List<SavedPlace> = previewSavedPlaces,
    recentDestinations: List<RecentDestination> = previewRecentDestinations,
) {
    AutoVerseTheme(themeId = themeId, appearanceMode = appearanceMode) {
        NavigationScreen(
            resolved = ResolvedNavigationProvider(
                provider = NavigationProvider.GoogleMaps,
                preferredMissing = false,
            ),
            savedPlaces = savedPlaces,
            recentDestinations = recentDestinations,
            embeddedMapEnabled = false,
            query = query,
            onQueryChange = {},
            placesPanelVisible = placesPanelVisible,
            onTogglePlacesPanel = {},
            onNavigate = { NavigationOutcome.OPENED },
            onOpenNavigationApp = { NavigationOutcome.OPENED },
            onSavePlace = { _, _ -> },
            onRemovePlace = {},
            onChangeProvider = {},
            onBack = {},
        )
    }
}

@Preview(name = "Navigation — Wide (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun NavigationPageWidePreview() {
    NavigationPagePreview(appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Navigation — Wide, Day (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun NavigationPageWideDayPreview() {
    NavigationPagePreview(appearanceMode = AppearanceMode.DAY)
}

@Preview(name = "Navigation — Wide, panel hidden (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun NavigationPageWidePanelHiddenPreview() {
    NavigationPagePreview(
        appearanceMode = AppearanceMode.NIGHT,
        placesPanelVisible = false,
    )
}

@Preview(name = "Navigation — Wide, no places yet (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun NavigationPageWideEmptyPlacesPreview() {
    NavigationPagePreview(
        appearanceMode = AppearanceMode.NIGHT,
        query = "",
        savedPlaces = emptyList(),
        recentDestinations = emptyList(),
    )
}

@Preview(name = "Navigation — Wide, invalid coordinates (1280x800)", widthDp = 1280, heightDp = 800)
@Composable
private fun NavigationPageWideInvalidCoordinatesPreview() {
    NavigationPagePreview(
        appearanceMode = AppearanceMode.NIGHT,
        query = "99, 99",
    )
}

@Preview(name = "Navigation — Medium (800x600)", widthDp = 800, heightDp = 600)
@Composable
private fun NavigationPageMediumPreview() {
    NavigationPagePreview(appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Navigation — Compact (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun NavigationPageCompactPreview() {
    NavigationPagePreview(appearanceMode = AppearanceMode.NIGHT)
}

@Preview(name = "Navigation — Compact, Day (411x891)", widthDp = 411, heightDp = 891)
@Composable
private fun NavigationPageCompactDayPreview() {
    NavigationPagePreview(appearanceMode = AppearanceMode.DAY)
}
