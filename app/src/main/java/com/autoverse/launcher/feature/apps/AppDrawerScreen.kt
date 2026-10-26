package com.autoverse.launcher.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.AppsLayoutRules
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.appsPanelsSideBySide
import com.autoverse.launcher.core.design.components.AppListRow
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.AppTile
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.DashboardTopBar
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.SectionLabel
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.ThemedAppIcon
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.launcher.AppCatalog
import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.launcher.LaunchOutcome
import com.autoverse.launcher.core.launcher.launchFailureMessage
import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.InstalledApp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The Apps page: All Apps (~70%) beside Recently Used (~30%). There is no
 * map, navigation or destination content here. Recently Used is AutoVerse's
 * own launch history (apps started from AutoVerse), recorded locally — it is
 * not device-wide usage, which would need Usage Access. Collapsing Recently
 * Used gives All Apps the full width on the same frame; the choice persists.
 */
@Composable
fun AppDrawerScreen(
    clockText: String,
    dateText: String,
    connectivity: ConnectivityStatus,
    state: AppListState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    category: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    onLaunchApp: suspend (InstalledApp) -> LaunchOutcome,
    onRefresh: () -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSpeedometer: () -> Unit = {},
    onOpenObd: () -> Unit = {},
    recentApps: List<InstalledApp> = emptyList(),
    recentCollapsed: Boolean = false,
    onRecentCollapsedChange: (Boolean) -> Unit = {},
    onClearRecent: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val handleLaunch: (InstalledApp) -> Unit = { app ->
        scope.launch {
            val outcome = onLaunchApp(app)
            if (outcome != LaunchOutcome.LAUNCHED) {
                snackbarHostState.showSnackbar(launchFailureMessage(app.label, outcome), withDismissAction = true)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val colors = LocalAutoVerseColors.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(colors.backgroundTop)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = AutoVerseSpacing.md, vertical = AutoVerseSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            DashboardTopBar(
                clockText = clockText,
                dateText = dateText,
                connectivity = connectivity,
                onOpenDiagnostics = onOpenDiagnostics,
                onOpenSettings = onOpenSettings,
            )
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                AppNavigationRail(
                    selected = RailDestination.APPS,
                    onOpenHome = onOpenHome,
                    onOpenAppDrawer = {},
                    onOpenNavigation = onOpenNavigation,
                    onOpenSpeedometer = onOpenSpeedometer,
                    onOpenObd = onOpenObd,
                    onOpenDiagnostics = onOpenDiagnostics,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.width(AutoVerseSpacing.railWidth).fillMaxHeight(),
                )
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (state) {
                        AppListState.Loading -> DrawerLoading(Modifier.fillMaxSize())
                        is AppListState.Error -> DrawerError(state.message, onRefresh, Modifier.fillMaxSize())
                        is AppListState.Ready -> AppsPanels(
                            apps = state.apps,
                            recentApps = recentApps,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            category = category,
                            onCategoryChange = onCategoryChange,
                            onLaunch = handleLaunch,
                            loadIcon = loadIcon,
                            onRefresh = onRefresh,
                            recentCollapsed = recentCollapsed,
                            onRecentCollapsedChange = onRecentCollapsedChange,
                            onClearRecent = onClearRecent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppsPanels(
    apps: List<InstalledApp>,
    recentApps: List<InstalledApp>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    category: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    onRefresh: () -> Unit,
    recentCollapsed: Boolean,
    onRecentCollapsedChange: (Boolean) -> Unit,
    onClearRecent: () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sideBySide = appsPanelsSideBySide(maxWidth.value.roundToInt())
        val allApps: @Composable (Modifier) -> Unit = { panelModifier ->
            AllAppsPanel(
                apps = apps,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                category = category,
                onCategoryChange = onCategoryChange,
                onLaunch = onLaunch,
                loadIcon = loadIcon,
                onRefresh = onRefresh,
                restoreRecent = if (recentCollapsed) ({ onRecentCollapsedChange(false) }) else null,
                modifier = panelModifier,
            )
        }
        if (sideBySide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                allApps(Modifier.weight(AppsLayoutRules.ALL_APPS_FRACTION).fillMaxHeight())
                if (!recentCollapsed) {
                    RecentAppsPanel(
                        recentApps = recentApps,
                        onLaunch = onLaunch,
                        loadIcon = loadIcon,
                        onCollapse = { onRecentCollapsedChange(true) },
                        onClear = onClearRecent,
                        horizontal = false,
                        modifier = Modifier.weight(1f - AppsLayoutRules.ALL_APPS_FRACTION).fillMaxHeight(),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                if (!recentCollapsed) {
                    RecentAppsPanel(
                        recentApps = recentApps,
                        onLaunch = onLaunch,
                        loadIcon = loadIcon,
                        onCollapse = { onRecentCollapsedChange(true) },
                        onClear = onClearRecent,
                        horizontal = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                allApps(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AllAppsPanel(
    apps: List<InstalledApp>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    category: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    onRefresh: () -> Unit,
    restoreRecent: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    CockpitCard(modifier = modifier, contentPadding = PaddingValues(AutoVerseSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            SectionLabel(icon = Icons.Outlined.Apps, label = "ALL APPS · ${apps.size}")
            SearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
            )
            if (restoreRecent != null) {
                IconButton(onClick = restoreRecent) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronLeft,
                        contentDescription = "Show recently used",
                        tint = colors.textSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        CategoryChips(selected = category, onSelect = onCategoryChange)
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        val filtered = remember(apps, searchQuery, category) { AppCatalog.filter(apps, searchQuery, category) }
        when {
            apps.isEmpty() -> DrawerEmpty(onRefresh = onRefresh, modifier = Modifier.fillMaxWidth().weight(1f))
            filtered.isEmpty() -> Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "No apps match “${searchQuery.trim()}”"
                    } else {
                        "No apps in ${category.label}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = AppsLayoutRules.TILE_MIN_WIDTH_DP.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                items(filtered, key = { it.key }) { app ->
                    val icon by produceState<ImageBitmap?>(initialValue = null, app.key) { value = loadIcon(app) }
                    AppTile(label = app.label, icon = icon, onClick = { onLaunch(app) })
                }
            }
        }
    }
}

/**
 * AutoVerse's own recent launches, newest first, deduplicated and bounded by
 * the repository. Uninstalled apps drop out because entries resolve against
 * the live catalog.
 */
@Composable
private fun RecentAppsPanel(
    recentApps: List<InstalledApp>,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    onCollapse: () -> Unit,
    onClear: () -> Unit,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    CockpitCard(modifier = modifier, contentPadding = PaddingValues(AutoVerseSpacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(icon = Icons.Outlined.History, label = "RECENTLY USED", modifier = Modifier.weight(1f))
            if (recentApps.isNotEmpty()) TextAction(text = "Clear", onClick = onClear)
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = if (horizontal) Icons.Outlined.Close else Icons.Outlined.ChevronRight,
                    contentDescription = "Hide recently used",
                    tint = colors.textSecondary,
                )
            }
        }
        Text(
            text = "Apps opened from AutoVerse",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textFaint,
        )
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        if (recentApps.isEmpty()) {
            Text(
                text = "Apps you open from AutoVerse will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(vertical = AutoVerseSpacing.xs),
            )
            return@CockpitCard
        }
        if (horizontal) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
                items(recentApps, key = { it.key }) { app ->
                    RecentChip(app = app, onLaunch = onLaunch, loadIcon = loadIcon)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
            ) {
                items(recentApps, key = { it.key }) { app ->
                    val icon by produceState<ImageBitmap?>(initialValue = null, app.key) { value = loadIcon(app) }
                    AppListRow(label = app.label, icon = icon, onClick = { onLaunch(app) })
                }
            }
        }
    }
}

@Composable
private fun RecentChip(
    app: InstalledApp,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
) {
    val colors = LocalAutoVerseColors.current
    val icon by produceState<ImageBitmap?>(initialValue = null, app.key) { value = loadIcon(app) }
    Row(
        modifier = Modifier
            .clip(AutoVerseShapes.pill)
            .background(colors.surfaceHighlight)
            .selectable(selected = false, role = Role.Button, onClick = { onLaunch(app) })
            .heightIn(min = AutoVerseSpacing.touchMin)
            .padding(horizontal = AutoVerseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        ThemedAppIcon(bitmap = icon, fallback = Icons.Outlined.Apps, size = 28.dp, contentDescription = null)
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CategoryChips(
    selected: AppCategory,
    onSelect: (AppCategory) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
        items(AppCategory.entries, key = { it.name }) { entry ->
            CategoryChip(label = entry.label, selected = entry == selected, onClick = { onSelect(entry) })
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Box(
        modifier = Modifier
            .clip(AutoVerseShapes.pill)
            .background(if (selected) colors.accentSoft else colors.surface)
            .border(
                width = AutoVerseBorders.hairline,
                color = if (selected) colors.accent else colors.outline,
                shape = AutoVerseShapes.pill,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = AutoVerseSpacing.touchMin)
            .padding(horizontal = AutoVerseSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        singleLine = true,
        placeholder = { Text(text = "Search apps", style = MaterialTheme.typography.bodyLarge) },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(AutoVerseIcons.compact))
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

@Composable
private fun DrawerLoading(modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
        ) {
            CircularProgressIndicator(color = colors.accent)
            Text(text = "Loading apps…", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

@Composable
private fun DrawerError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(AutoVerseIcons.prominent),
                tint = colors.warning,
            )
            Text(
                text = "Couldn’t load apps",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            PrimaryAction(text = "Retry", onClick = onRetry)
        }
    }
}

@Composable
private fun DrawerEmpty(onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            Text(
                text = "No launchable apps found",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            PrimaryAction(text = "Refresh", onClick = onRefresh)
        }
    }
}
