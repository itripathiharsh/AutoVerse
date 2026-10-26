package com.autoverse.launcher.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.BuildConfig
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.SettingsPaneMode
import com.autoverse.launcher.core.design.StatusChipsMode
import com.autoverse.launcher.core.design.components.AppListRow
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.BrandPlate
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.DashboardTopBar
import com.autoverse.launcher.core.design.components.KeyValueRow
import com.autoverse.launcher.core.design.components.PremiumUnlockDialog
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.SelectRow
import com.autoverse.launcher.core.design.components.SliderRow
import com.autoverse.launcher.core.design.components.SwitchRow
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.settingsPanePolicyFor
import com.autoverse.launcher.core.design.themeSpecFor
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.performance.LocalPerformanceSpec
import com.autoverse.launcher.core.preferences.TEXT_SCALE_MAX
import com.autoverse.launcher.core.preferences.TEXT_SCALE_MIN
import com.autoverse.launcher.core.preferences.textScalePercent
import kotlin.math.roundToInt

/**
 * Discrete stops on the text-size slider: the 0.85–1.30 range in 0.05 steps,
 * i.e. eight stops strictly between the endpoints.
 */
private const val TEXT_SCALE_STEPS = 8

/**
 * Phase 16 — the settings surface.
 *
 * Three responsive presentations share one behavior, chosen by
 * [settingsPanePolicyFor] from the real window size and system font scale:
 * WIDE places the rail, a category pane, the controls, and a live dashboard
 * preview side by side; MEDIUM keeps the category pane beside the controls
 * with the preview below them; COMPACT stacks search, category chips, and
 * controls in a single scrollable column.
 *
 * The preview is a real render of the dashboard passed in as
 * [dashboardPreview] — the caller owns the live data and Home composition, so
 * this screen stays decoupled from dashboard internals while the preview
 * still follows every theme/appearance/text-size change immediately.
 *
 * The search field filters [SETTINGS_CATALOG] and results jump straight to
 * the owning category. Every visible control maps to a real, persisted
 * setting; unsupported concepts are deliberately absent instead of faked.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSetTheme: (AutoVerseThemeId) -> Unit,
    onSetPremiumUnlocked: (Boolean) -> Unit = {},
    onSetThemeSoundsEnabled: (Boolean) -> Unit = {},
    onReplayOnboarding: () -> Unit = {},
    onSetAppearanceMode: (AppearanceMode) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSetTextScale: (Float) -> Unit,
    onSetProfile: (PerformanceProfile) -> Unit,
    onSetPreferSmoothness: (Boolean) -> Unit,
    onSetClock24h: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    navigationProviders: List<NavigationProvider> = emptyList(),
    onSetNavigationPackage: (String?) -> Unit = {},
    notificationAccessGranted: Boolean = false,
    onOpenNotificationAccessSettings: () -> Boolean = { false },
    favoriteApps: List<InstalledApp> = emptyList(),
    onLaunchApp: (InstalledApp) -> Unit = {},
    onRemoveFavorite: (InstalledApp) -> Unit = {},
    onOpenAppDrawer: () -> Unit = {},
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap? = { null },
    onOpenDiagnostics: () -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenNavigation: () -> Unit = {},
    onOpenSpeedometer: () -> Unit = {},
    onOpenObd: () -> Unit = {},
    clockText: String = "",
    dateText: String = "",
    connectivity: ConnectivityStatus = ConnectivityStatus(),
    dashboardPreview: @Composable (Modifier) -> Unit = {},
    onResetSettings: () -> Unit = {},
    initialCategory: SettingsCategory? = null,
) {
    var selectedCategoryName by rememberSaveable {
        mutableStateOf((initialCategory ?: SettingsCategory.APPEARANCE).name)
    }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    val selectedCategory = SettingsCategory.entries
        .firstOrNull { it.name == selectedCategoryName }
        ?: SettingsCategory.APPEARANCE

    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current

    val callbacks = SettingsCallbacks(
        setTheme = onSetTheme,
        setPremiumUnlocked = onSetPremiumUnlocked,
        setThemeSoundsEnabled = onSetThemeSoundsEnabled,
        runOnboarding = onReplayOnboarding,
        setAppearanceMode = onSetAppearanceMode,
        setReducedMotion = onSetReducedMotion,
        setTextScale = onSetTextScale,
        setProfile = onSetProfile,
        setPreferSmoothness = onSetPreferSmoothness,
        setClock24h = onSetClock24h,
        setNavigationPackage = onSetNavigationPackage,
        openNotificationAccessSettings = onOpenNotificationAccessSettings,
        launchApp = onLaunchApp,
        removeFavorite = onRemoveFavorite,
        openAppDrawer = onOpenAppDrawer,
        loadAppIcon = loadAppIcon,
        openDiagnostics = onOpenDiagnostics,
        resetSettings = onResetSettings,
        openHome = onOpenHome,
        openNavigation = onOpenNavigation,
        openSpeedometer = onOpenSpeedometer,
        openObd = onOpenObd,
    )

    val selectCategory: (SettingsCategory) -> Unit = { category ->
        selectedCategoryName = category.name
        searchQuery = ""
    }

    val topBar: @Composable (Boolean) -> Unit = { showLogo ->
        DashboardTopBar(
            clockText = clockText,
            dateText = dateText,
            connectivity = connectivity,
            chips = StatusChipsMode.NONE,
            showLogo = showLogo,
            onBack = onBack,
            onOpenDiagnostics = onOpenDiagnostics,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (spec.flatSurfaces) {
                    Modifier.background(colors.backgroundTop)
                } else {
                    Modifier.background(
                        Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)),
                    )
                },
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = AutoVerseSpacing.screenHorizontal,
                vertical = AutoVerseSpacing.screenVertical,
            ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val paneMode = settingsPanePolicyFor(
                widthDp = maxWidth.value.roundToInt(),
                heightDp = maxHeight.value.roundToInt(),
                fontScale = density.fontScale,
            )
            when (paneMode) {
                SettingsPaneMode.WIDE -> Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                ) {
                    AppNavigationRail(
                        selected = RailDestination.SETTINGS,
                        onOpenHome = callbacks.openHome,
                        onOpenAppDrawer = callbacks.openAppDrawer,
                        onOpenNavigation = callbacks.openNavigation,
                        onOpenSpeedometer = callbacks.openSpeedometer,
                        onOpenObd = callbacks.openObd,
                        onOpenDiagnostics = callbacks.openDiagnostics,
                        onOpenSettings = {},
                        modifier = Modifier
                            .width(AutoVerseSpacing.railWidth)
                            .fillMaxHeight(),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                    ) {
                        topBar(false)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                        ) {
                            CategoriesPane(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedCategory = selectedCategory,
                                onSelectCategory = selectCategory,
                                modifier = Modifier
                                    .width(260.dp)
                                    .fillMaxHeight(),
                            )
                            ControlsContent(
                                category = selectedCategory,
                                searchQuery = searchQuery,
                                onSelectCategory = selectCategory,
                                settings = settings,
                                navigationProviders = navigationProviders,
                                notificationAccessGranted = notificationAccessGranted,
                                favoriteApps = favoriteApps,
                                callbacks = callbacks,
                                preview = dashboardPreview,
                                previewHeight = 340.dp,
                                includeSecondary = false,
                                onShowResetRequest = { showResetDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState()),
                            )
                            SecondaryPane(
                                preview = dashboardPreview,
                                onShowResetRequest = { showResetDialog = true },
                                modifier = Modifier
                                    .width(340.dp)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }

                SettingsPaneMode.MEDIUM -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                ) {
                    topBar(true)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                    ) {
                        CategoriesPane(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onSelectCategory = selectCategory,
                            modifier = Modifier
                                .width(240.dp)
                                .fillMaxHeight(),
                        )
                        ControlsContent(
                            category = selectedCategory,
                            searchQuery = searchQuery,
                            onSelectCategory = selectCategory,
                            settings = settings,
                            navigationProviders = navigationProviders,
                            notificationAccessGranted = notificationAccessGranted,
                            favoriteApps = favoriteApps,
                            callbacks = callbacks,
                            preview = dashboardPreview,
                            previewHeight = 280.dp,
                            includeSecondary = true,
                            onShowResetRequest = { showResetDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }

                SettingsPaneMode.COMPACT -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                ) {
                    topBar(true)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
                    ) {
                        SettingsSearchField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                        )
                        CategoryChipsRow(
                            selectedCategory = selectedCategory,
                            onSelectCategory = selectCategory,
                        )
                        ControlsContent(
                            category = selectedCategory,
                            searchQuery = searchQuery,
                            onSelectCategory = selectCategory,
                            settings = settings,
                            navigationProviders = navigationProviders,
                            notificationAccessGranted = notificationAccessGranted,
                            favoriteApps = favoriteApps,
                            callbacks = callbacks,
                            preview = dashboardPreview,
                            previewHeight = 280.dp,
                            includeSecondary = true,
                            onShowResetRequest = { showResetDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset settings?") },
            text = {
                Text(
                    "Theme, appearance, text size, motion, performance, " +
                        "clock, and navigation choices return to their defaults. " +
                        "Favorite apps are kept.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onResetSettings()
                    },
                ) {
                    Text("Reset", color = LocalAutoVerseColors.current.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/**
 * Bundles every action the settings pane can dispatch. Passing one holder
 * down the layout tree keeps the pane composables to a single callback
 * parameter instead of twenty. Rebuilt each composition on purpose: the
 * lambdas must stay fresh to capture the latest state from the caller.
 */
private class SettingsCallbacks(
    val setTheme: (AutoVerseThemeId) -> Unit,
    val setPremiumUnlocked: (Boolean) -> Unit,
    val setThemeSoundsEnabled: (Boolean) -> Unit,
    val runOnboarding: () -> Unit,
    val setAppearanceMode: (AppearanceMode) -> Unit,
    val setReducedMotion: (Boolean) -> Unit,
    val setTextScale: (Float) -> Unit,
    val setProfile: (PerformanceProfile) -> Unit,
    val setPreferSmoothness: (Boolean) -> Unit,
    val setClock24h: (Boolean) -> Unit,
    val setNavigationPackage: (String?) -> Unit,
    val openNotificationAccessSettings: () -> Boolean,
    val launchApp: (InstalledApp) -> Unit,
    val removeFavorite: (InstalledApp) -> Unit,
    val openAppDrawer: () -> Unit,
    val loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
    val openDiagnostics: () -> Unit,
    val resetSettings: () -> Unit,
    val openHome: () -> Unit,
    val openNavigation: () -> Unit,
    val openSpeedometer: () -> Unit,
    val openObd: () -> Unit,
)

/**
 * Category navigation with the search field on top. Used by the WIDE and
 * MEDIUM presentations; both pass their remaining pane height so the item
 * list scrolls when the window is short.
 */
@Composable
private fun CategoriesPane(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCategory: SettingsCategory,
    onSelectCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    CockpitCard(modifier = modifier) {
        SettingsSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
        )
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
        ) {
            SettingsCategory.entries.forEach { category ->
                CategoryNavItem(
                    category = category,
                    selected = category == selectedCategory,
                    onClick = { onSelectCategory(category) },
                )
            }
        }
    }
}

/**
 * Vertical category entry for the category pane: icon + label, accent-tinted
 * when selected. Mirrors the rail's selection language (RadioButton role).
 */
@Composable
private fun CategoryNavItem(
    category: SettingsCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.tile)
            .background(if (selected) colors.accentSoft else Color.Transparent)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .heightIn(min = AutoVerseSpacing.touchComfort)
            .padding(horizontal = AutoVerseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.compact),
            tint = if (selected) colors.accent else colors.textSecondary,
        )
        Text(
            text = category.label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Horizontal category chips for the COMPACT presentation. */
@Composable
private fun CategoryChipsRow(
    selectedCategory: SettingsCategory,
    onSelectCategory: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        items(SettingsCategory.entries, key = { it.name }) { category ->
            CategoryChip(
                category = category,
                selected = category == selectedCategory,
                onClick = { onSelectCategory(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: SettingsCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .clip(AutoVerseShapes.pill)
            .background(if (selected) colors.accentSoft else colors.surface)
            .border(
                width = AutoVerseBorders.hairline,
                color = if (selected) colors.accent else colors.outline,
                shape = AutoVerseShapes.pill,
            )
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .heightIn(min = AutoVerseSpacing.touchMin)
            .padding(horizontal = AutoVerseSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        Icon(
            imageVector = categoryIcon(category),
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.inline),
            tint = if (selected) colors.accent else colors.textFaint,
        )
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
    }
}

/**
 * The center column: search results while a query is active, otherwise the
 * selected category's header and real cards. [includeSecondary] appends the
 * preview and reset cards for the MEDIUM/COMPACT presentations where no
 * separate secondary pane exists; the WIDE presentation renders them in the
 * dedicated column instead.
 */
@Composable
private fun ControlsContent(
    category: SettingsCategory,
    searchQuery: String,
    onSelectCategory: (SettingsCategory) -> Unit,
    settings: AppSettings,
    navigationProviders: List<NavigationProvider>,
    notificationAccessGranted: Boolean,
    favoriteApps: List<InstalledApp>,
    callbacks: SettingsCallbacks,
    preview: @Composable (Modifier) -> Unit,
    previewHeight: Dp,
    includeSecondary: Boolean,
    onShowResetRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
    ) {
        if (searchQuery.isNotBlank()) {
            SearchResults(
                query = searchQuery,
                onOpenEntry = { entry -> onSelectCategory(entry.category) },
            )
        } else {
            Text(
                text = category.label,
                style = MaterialTheme.typography.headlineSmall,
                color = LocalAutoVerseColors.current.textPrimary,
            )
            when (category) {
                SettingsCategory.APPEARANCE -> AppearanceSettings(settings, callbacks)
                SettingsCategory.NAVIGATION ->
                    NavigationSettings(settings, navigationProviders, callbacks)
                SettingsCategory.MEDIA_AUDIO ->
                    MediaAudioSettings(notificationAccessGranted, callbacks)
                SettingsCategory.APPS_SHORTCUTS ->
                    AppsShortcutsSettings(favoriteApps, callbacks)
                SettingsCategory.PERFORMANCE -> PerformanceSettings(settings, callbacks)
                SettingsCategory.NOTIFICATIONS ->
                    NotificationsSettings(notificationAccessGranted, callbacks)
                SettingsCategory.ABOUT -> AboutSettings(onReplayOnboarding = callbacks.runOnboarding)
            }
        }
        if (includeSecondary) {
            SettingsPreviewCard(preview = preview, previewHeight = previewHeight)
            SettingsResetCard(onReset = onShowResetRequest)
        }
    }
}

/** Right-hand column of the WIDE presentation: live preview + reset. */
@Composable
private fun SecondaryPane(
    preview: @Composable (Modifier) -> Unit,
    onShowResetRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
    ) {
        SettingsPreviewCard(preview = preview, previewHeight = 340.dp)
        SettingsResetCard(onReset = onShowResetRequest)
    }
}

/**
 * Wraps the real dashboard render passed by the caller. The preview is a
 * live composition — it updates the moment a theme, appearance, or text-size
 * change is committed — never a screenshot or a static mock.
 */
@Composable
private fun SettingsPreviewCard(
    preview: @Composable (Modifier) -> Unit,
    previewHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "DASHBOARD PREVIEW",
        subtitle = "Live \u2014 follows the theme, appearance, and text size you set here",
        icon = Icons.Outlined.Dashboard,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewHeight)
                .clip(AutoVerseShapes.tile)
                .border(
                    width = AutoVerseBorders.hairline,
                    color = colors.outline,
                    shape = AutoVerseShapes.tile,
                ),
        ) {
            preview(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SettingsResetCard(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "RESET",
        icon = Icons.Outlined.Restore,
        modifier = modifier,
    ) {
        Text(
            text = "Restores every setting on this page to its default. " +
                "Favorite apps are kept.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        TextAction(text = "Reset settings to defaults", onClick = onReset)
    }
}

@Composable
private fun SearchResults(
    query: String,
    onOpenEntry: (SettingsEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val results = searchSettings(query)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
    ) {
        Text(
            text = "Results for \u201c$query\u201d",
            style = MaterialTheme.typography.titleMedium,
            color = colors.textSecondary,
        )
        if (results.isEmpty()) {
            WidgetContainer(title = "NO MATCHES", icon = Icons.Outlined.Search) {
                Text(
                    text = "No settings match \u201c$query\u201d. " +
                        "Try a shorter or different word.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        } else {
            WidgetContainer(title = "SETTINGS", icon = Icons.Outlined.Search) {
                results.forEach { entry ->
                    SearchResultRow(entry = entry, onClick = { onOpenEntry(entry) })
                }
            }
        }
    }
}

/**
 * One search hit: category icon, title, and where it lives. Tapping jumps to
 * the owning category (and the caller clears the query), matching the way a
 * user would find the control themselves.
 */
@Composable
private fun SearchResultRow(
    entry: SettingsEntry,
    onClick: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.row)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = AutoVerseSpacing.touchMin)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.inlineGap),
    ) {
        Icon(
            imageVector = categoryIcon(entry.category),
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.standard),
            tint = colors.textSecondary,
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = "${entry.category.label} \u00b7 ${entry.subtitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AppearanceSettings(
    settings: AppSettings,
    callbacks: SettingsCallbacks,
) {
    val colors = LocalAutoVerseColors.current
    var showPremiumDialog by remember { mutableStateOf(false) }

    WidgetContainer(
        title = "THEME",
        subtitle = "One design language, ten characters",
        icon = Icons.Outlined.DarkMode,
    ) {
        AutoVerseThemeId.entries.forEach { theme ->
            val locked = theme.premium && !settings.premiumUnlocked
            SelectRow(
                title = theme.displayName,
                subtitle = themeDescription(theme) + if (theme.premium) " · Premium" else "",
                selected = theme == settings.themeId,
                enabled = true,
                onSelect = {
                    if (locked) showPremiumDialog = true else callbacks.setTheme(theme)
                },
                leading = { ThemeSwatch(theme) },
            )
        }
    }

    if (showPremiumDialog) {
        PremiumUnlockDialog(
            onUnlock = {
                callbacks.setPremiumUnlocked(true)
                showPremiumDialog = false
            },
            onDismiss = { showPremiumDialog = false },
        )
    }

    WidgetContainer(
        title = "SOUND",
        subtitle = "Synthesized locally — off by default, never over navigation",
        icon = Icons.Outlined.VolumeUp,
    ) {
        SwitchRow(
            title = "Theme confirmation sounds",
            subtitle = "A short local tone when a theme is applied",
            checked = settings.themeSoundsEnabled,
            onCheckedChange = callbacks.setThemeSoundsEnabled,
        )
    }

    WidgetContainer(
        title = "DAY / NIGHT",
        subtitle = "Same layout, legible in any cabin light",
        icon = Icons.Outlined.LightMode,
    ) {
        AppearanceMode.entries.forEach { mode ->
            SelectRow(
                title = mode.displayName,
                subtitle = appearanceDescription(mode),
                selected = mode == settings.appearanceMode,
                enabled = true,
                onSelect = { callbacks.setAppearanceMode(mode) },
            )
        }
    }

    WidgetContainer(
        title = "DISPLAY & LAYOUT",
        subtitle = "Text size and adaptive dashboard layout",
        icon = Icons.Outlined.FitScreen,
    ) {
        SliderRow(
            title = "Text size",
            subtitle = "Scales interface text across the app",
            value = settings.textScale,
            valueRange = TEXT_SCALE_MIN..TEXT_SCALE_MAX,
            valueText = "${textScalePercent(settings.textScale)}%",
            onValueChange = callbacks.setTextScale,
            stepCount = TEXT_SCALE_STEPS,
        )
        if (settings.textScale != 1f) {
            TextAction(text = "Reset text size", onClick = { callbacks.setTextScale(1f) })
        }
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        KeyValueRow("Adaptive layout", "Automatic (Recommended)")
        Spacer(Modifier.height(AutoVerseSpacing.xxs))
        Text(
            text = "The dashboard picks its layout from the window size and " +
                "system text scale \u2014 there is no manual override to misconfigure.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textFaint,
        )
    }

    WidgetContainer(
        title = "CLOCK",
        subtitle = "Time format on the dashboard and top bar",
        icon = Icons.Outlined.Tune,
    ) {
        SwitchRow(
            title = "24-hour time",
            subtitle = "Use a 24-hour clock",
            checked = settings.clock24h,
            onCheckedChange = callbacks.setClock24h,
        )
    }

    WidgetContainer(
        title = "MOTION",
        subtitle = "Cuts dashboard animation for a steadier cabin view",
        icon = Icons.Outlined.Animation,
    ) {
        SwitchRow(
            title = "Reduce motion",
            subtitle = "Instant screen changes, no animated transitions",
            checked = settings.reducedMotion,
            onCheckedChange = callbacks.setReducedMotion,
        )
    }
}

/**
 * Theme swatches show each palette's own charcoal background with its accent
 * ring and core — the dark variant on purpose, because that is the cabin
 * character each theme is built around in every appearance mode.
 */
@Composable
private fun ThemeSwatch(themeId: AutoVerseThemeId) {
    val palette = themeSpecFor(themeId).darkColors
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(palette.backgroundTop)
            .border(
                width = AutoVerseBorders.hairline,
                color = palette.accent,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(palette.accent),
        )
    }
}

@Composable
private fun NavigationSettings(
    settings: AppSettings,
    navigationProviders: List<NavigationProvider>,
    callbacks: SettingsCallbacks,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "NAVIGATION APP",
        subtitle = "Hands destinations to your preferred app",
        icon = Icons.Outlined.Navigation,
    ) {
        SelectRow(
            title = NavigationProvider.SystemDefault.label,
            subtitle = "Opens whichever app handles the geo: hand-off",
            selected = settings.navigationPackage == null,
            enabled = true,
            onSelect = { callbacks.setNavigationPackage(null) },
        )
        navigationProviders.forEach { provider ->
            SelectRow(
                title = provider.label,
                subtitle = if (provider.supportsDestination) {
                    "Receives address and coordinate destinations"
                } else {
                    "Opens the app only \u2014 no documented destination format"
                },
                selected = settings.navigationPackage == provider.packageName,
                enabled = true,
                onSelect = { callbacks.setNavigationPackage(provider.packageName) },
            )
        }
        val preferredPackage = settings.navigationPackage
        val preferredMissing = preferredPackage != null &&
            navigationProviders.none { it.packageName == preferredPackage }
        if (preferredMissing) {
            Spacer(Modifier.height(AutoVerseSpacing.xxs))
            Text(
                text = "Preferred app isn\u2019t installed \u2014 using the system default",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.warning,
            )
        }
    }
}

@Composable
private fun MediaAudioSettings(
    notificationAccessGranted: Boolean,
    callbacks: SettingsCallbacks,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "MEDIA ACCESS",
        subtitle = "Powers the dashboard media card",
        icon = Icons.Outlined.MusicNote,
    ) {
        NotificationAccessBlock(
            granted = notificationAccessGranted,
            explanation = "The dashboard media card reads the active media " +
                "session through Android\u2019s notification-listener access. " +
                "Without it, the card shows a hint instead of playback information.",
            callbacks = callbacks,
        )
    }

    WidgetContainer(
        title = "MEDIA CARD",
        subtitle = "What the dashboard shows",
        icon = Icons.Outlined.Dashboard,
    ) {
        Text(
            text = "Playback controls and artwork mirror whatever the active " +
                "media app reports \u2014 AutoVerse never hosts its own player. " +
                "If no session is active, the card explains how to start one.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

@Composable
private fun AppsShortcutsSettings(
    favoriteApps: List<InstalledApp>,
    callbacks: SettingsCallbacks,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "FAVORITE APPS",
        subtitle = "Shown on the dashboard and in the app drawer",
        icon = Icons.Outlined.Apps,
    ) {
        if (favoriteApps.isEmpty()) {
            Text(
                text = "No favorites yet. Open the app drawer and tap the star " +
                    "on any app to pin it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        } else {
            favoriteApps.forEach { app ->
                FavoriteAppRow(
                    app = app,
                    onClick = { callbacks.launchApp(app) },
                    onToggleFavorite = { callbacks.removeFavorite(app) },
                    loadAppIcon = callbacks.loadAppIcon,
                )
            }
        }
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        TextAction(text = "Open app drawer", onClick = callbacks.openAppDrawer)
    }
}

/**
 * One favorite: real icon (loaded through the caller's repository-backed
 * loader), real launch, and the same star toggle the drawer uses so both
 * surfaces stay in sync through the single favorites store.
 */
@Composable
private fun FavoriteAppRow(
    app: InstalledApp,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    loadAppIcon: suspend (InstalledApp) -> ImageBitmap?,
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, app.key) {
        value = loadAppIcon(app)
    }
    AppListRow(
        label = app.label,
        onClick = onClick,
        icon = icon,
        isFavorite = true,
        onToggleFavorite = onToggleFavorite,
    )
}

@Composable
private fun PerformanceSettings(
    settings: AppSettings,
    callbacks: SettingsCallbacks,
) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "PERFORMANCE PROFILE",
        subtitle = "Balances motion and effects against available headroom",
        icon = Icons.Outlined.Speed,
    ) {
        PerformanceProfile.entries.forEach { profile ->
            SelectRow(
                title = profile.displayName,
                subtitle = profileDescription(profile),
                selected = profile == settings.profile,
                enabled = true,
                onSelect = { callbacks.setProfile(profile) },
            )
        }
        SwitchRow(
            title = "Prefer smoothness",
            subtitle = "Runs the dashboard with the Constrained workload",
            checked = settings.preferSmoothness,
            onCheckedChange = callbacks.setPreferSmoothness,
        )
    }

    val effectiveProfile = if (settings.preferSmoothness) {
        PerformanceProfile.CONSTRAINED
    } else {
        settings.profile
    }
    WidgetContainer(
        title = "EFFECTIVE WORKLOAD",
        subtitle = "What the dashboard is running right now",
        icon = Icons.Outlined.Insights,
    ) {
        KeyValueRow("Selected", settings.profile.displayName)
        KeyValueRow("Effective", effectiveProfile.displayName)
        if (settings.preferSmoothness) {
            Spacer(Modifier.height(AutoVerseSpacing.xxs))
            Text(
                text = "Prefer smoothness is on, so the Constrained workload " +
                    "runs regardless of the selected profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        TextAction(text = "Open diagnostics", onClick = callbacks.openDiagnostics)
    }
}

@Composable
private fun NotificationsSettings(
    notificationAccessGranted: Boolean,
    callbacks: SettingsCallbacks,
) {
    WidgetContainer(
        title = "NOTIFICATION ACCESS",
        subtitle = "Android notification-listener permission",
        icon = Icons.Outlined.Notifications,
    ) {
        NotificationAccessBlock(
            granted = notificationAccessGranted,
            explanation = "AutoVerse registers a notification listener for one " +
                "purpose: reading the active media session so the dashboard can " +
                "show now-playing info and transport controls. It never posts " +
                "notifications and ignores notification content.",
            callbacks = callbacks,
        )
    }
}

/**
 * Shared notification-access block used by both Media & Audio and
 * Notifications: real access state, honest explanation, and a route to the
 * system screen. If the platform refuses to open the screen, the failure is
 * surfaced inline instead of being swallowed.
 */
@Composable
private fun NotificationAccessBlock(
    granted: Boolean,
    explanation: String,
    callbacks: SettingsCallbacks,
) {
    val colors = LocalAutoVerseColors.current
    var openFailed by remember { mutableStateOf(false) }
    KeyValueRow("Access", if (granted) "Granted" else "Not granted")
    Spacer(Modifier.height(AutoVerseSpacing.xxs))
    Text(
        text = explanation,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
    )
    Spacer(Modifier.height(AutoVerseSpacing.xs))
    TextAction(
        text = "Open notification access settings",
        onClick = { openFailed = !callbacks.openNotificationAccessSettings() },
    )
    if (openFailed) {
        Spacer(Modifier.height(AutoVerseSpacing.xxs))
        Text(
            text = "Couldn\u2019t open system settings \u2014 open Android Settings " +
                "and search for \u201cNotification access\u201d.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.warning,
        )
    }
}

@Composable
private fun AboutSettings(onReplayOnboarding: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    WidgetContainer(
        title = "SAFETY",
        icon = Icons.Outlined.Warning,
    ) {
        Text(
            text = "Configure while parked \u2014 set themes, shortcuts, and " +
                "destinations before you set off. Do not interact with " +
                "the screen while driving, and obey local traffic laws.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }

    WidgetContainer(
        title = "AUTOVERSE",
        icon = Icons.Outlined.Info,
    ) {
        BrandPlate(logoHeight = 32.dp)
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        KeyValueRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        KeyValueRow("Build", if (BuildConfig.DEBUG) "Debug" else "Release")
        KeyValueRow("Stage", "Tier 1 — onboarding, driving mode, premium themes")
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        TextAction(
            text = "Run first-time setup again",
            onClick = onReplayOnboarding,
        )
    }

    WidgetContainer(
        title = "LICENSES",
        subtitle = "Open-source attribution for shipped components",
        icon = Icons.Outlined.Description,
    ) {
        Text(
            text = "Kotlin, AndroidX, Jetpack Compose, Okio, OkHttp, Gson, " +
                "and Timber ship under Apache-2.0. Leaflet (cockpit map) " +
                "ships under BSD-2-Clause. desugar_jdk_libs ships under " +
                "GPL-2.0 with the Classpath exception. JUnit (test-only, " +
                "not in the APK) is EPL-1.0.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        Text(
            text = "Full notice texts ship with the project\u2019s license " +
                "documentation in the source tree.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textFaint,
        )
    }

    WidgetContainer(
        title = "PRIVACY",
        subtitle = "What leaves this device",
        icon = Icons.Outlined.Lock,
    ) {
        Text(
            text = "No telemetry, analytics, advertising, or crash reporting. " +
                "Settings and favorites stay on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        Text(
            text = "Network access is used only by the experimental embedded " +
                "map, which is off by default.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textFaint,
        )
    }
}

@Composable
private fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                text = "Search settings",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(AutoVerseIcons.compact),
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear search",
                        modifier = Modifier.size(AutoVerseIcons.compact),
                    )
                }
            }
        } else {
            null
        },
        shape = AutoVerseShapes.tile,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.outline,
            focusedPlaceholderColor = colors.textFaint,
            unfocusedPlaceholderColor = colors.textFaint,
            cursorColor = colors.accent,
            focusedLeadingIconColor = colors.textSecondary,
            unfocusedLeadingIconColor = colors.textFaint,
            focusedTrailingIconColor = colors.textSecondary,
            unfocusedTrailingIconColor = colors.textFaint,
        ),
    )
}

private fun categoryIcon(category: SettingsCategory): ImageVector = when (category) {
    SettingsCategory.APPEARANCE -> Icons.Outlined.Palette
    SettingsCategory.NAVIGATION -> Icons.Outlined.Navigation
    SettingsCategory.MEDIA_AUDIO -> Icons.Outlined.MusicNote
    SettingsCategory.APPS_SHORTCUTS -> Icons.Outlined.Apps
    SettingsCategory.PERFORMANCE -> Icons.Outlined.Speed
    SettingsCategory.NOTIFICATIONS -> Icons.Outlined.Notifications
    SettingsCategory.ABOUT -> Icons.Outlined.Info
}

private fun themeDescription(theme: AutoVerseThemeId): String = when (theme) {
    AutoVerseThemeId.COBALT -> "Deep charcoal with the brand\u2019s electric blue"
    AutoVerseThemeId.OBSIDIAN -> "Graphite and silver \u2014 the AutoVerse baseline"
    AutoVerseThemeId.NEON_PULSE -> "Midnight blue with electric cyan accents"
    AutoVerseThemeId.ATELIER -> "Warm neutrals and quiet, calm surfaces"
    AutoVerseThemeId.APEX -> "Motorsport reds with stronger hierarchy"
    AutoVerseThemeId.SAKURA -> "Anime day-vibe: blush plum and cherry petals"
    AutoVerseThemeId.NEON_TOKYO -> "Anime night: midnight city with electric cyan"
    AutoVerseThemeId.SHONEN -> "Anime hero: ink indigo, plus-ultra energy"
    AutoVerseThemeId.GOTHAM -> "Vigilante stealth: true black, bat-signal yellow"
    AutoVerseThemeId.HOT_ROD -> "Armored hero: warm gunmetal and arc red"
    AutoVerseThemeId.FORTRESS -> "Bright citadel: warm white and sky blue"
    AutoVerseThemeId.WEB_RUSH -> "Urban sport: racing red and electric blue"
}

private fun appearanceDescription(mode: AppearanceMode): String = when (mode) {
    AppearanceMode.SYSTEM -> "Follow the device setting"
    AppearanceMode.DAY -> "Always use the light palette"
    AppearanceMode.NIGHT -> "Always use the dark palette"
}

private fun profileDescription(profile: PerformanceProfile): String = when (profile) {
    PerformanceProfile.CONSTRAINED -> "Minimal motion, lowest overhead"
    PerformanceProfile.BALANCED -> "Default — smooth with sensible effects"
    PerformanceProfile.ENHANCED -> "Full motion and richer live surfaces"
}
