package com.autoverse.launcher.feature.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.design.AppDrawerMode
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.StatusChipsMode
import com.autoverse.launcher.core.design.appDrawerLayoutPolicyFor
import com.autoverse.launcher.core.design.components.AppListRow
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.AppTile
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.DashboardTopBar
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.SectionLabel
import com.autoverse.launcher.core.design.components.TextAction
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
import com.autoverse.launcher.core.performance.LocalPerformanceSpec
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Drawer arrangement: adaptive icon grid or single-column list. */
enum class AppDrawerView { GRID, LIST }

/** How many recent installs the Newly Installed panel surfaces. */
private const val NEWLY_INSTALLED_LIMIT = 5

/** Width of the right-hand favorites/installs column in the expanded layout. */
private val RIGHT_COLUMN_WIDTH = 300.dp

/**
 * Full app drawer. Layout follows [appDrawerLayoutPolicyFor]: EXPANDED adds a
 * nav rail on the left and a favorites/newly-installed column on the right;
 * MEDIUM keeps the rail and folds the panels into the scrollable sections;
 * COMPACT drops the rail and relies on the top-bar back affordance.
 *
 * The reference design's "Recently Used" panel is deliberately not
 * implemented: per-app usage statistics require either the privileged
 * PACKAGE_USAGE_STATS permission or QUERY_ALL_PACKAGES-style enumeration,
 * both of which this launcher avoids. Newly Installed uses
 * PackageInfo.firstInstallTime, which needs no permission at all.
 *
 * Favorites editing stages changes in a local draft (plain `remember`, so it
 * deliberately does not survive process death — persisted favorites remain
 * the source of truth until Save).
 */
@Composable
fun AppDrawerScreen(
    clockText: String,
    dateText: String,
    connectivity: ConnectivityStatus,
    state: AppListState,
    favorites: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewMode: AppDrawerView,
    onViewModeChange: (AppDrawerView) -> Unit,
    category: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onSetFavorites: (List<String>) -> Unit,
    onLaunchApp: suspend (InstalledApp) -> LaunchOutcome,
    onRefresh: () -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    onBack: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val handleLaunch: (InstalledApp) -> Unit = { app ->
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

    var draft by remember { mutableStateOf<List<String>?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val colors = LocalAutoVerseColors.current
        val spec = LocalPerformanceSpec.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val insets = WindowInsets.safeDrawing
            val insetWidthDp = with(density) {
                (insets.getLeft(this, layoutDirection) + insets.getRight(this, layoutDirection)).toDp()
            }
            val insetHeightDp = with(density) {
                (insets.getTop(this) + insets.getBottom(this)).toDp()
            }
            val mode = appDrawerLayoutPolicyFor(
                widthDp = (maxWidth - insetWidthDp).value.roundToInt(),
                heightDp = (maxHeight - insetHeightDp).value.roundToInt(),
                fontScale = density.fontScale,
            )
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
                DashboardTopBar(
                    clockText = clockText,
                    dateText = dateText,
                    connectivity = connectivity,
                    chips = StatusChipsMode.NONE,
                    onBack = if (mode == AppDrawerMode.COMPACT) onBack else null,
                    onOpenDiagnostics = onOpenDiagnostics,
                    onOpenSettings = onOpenSettings,
                )
                when (state) {
                    AppListState.Loading -> DrawerLoading(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    is AppListState.Error -> DrawerError(
                        message = state.message,
                        onRetry = onRefresh,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    is AppListState.Ready -> DrawerBody(
                        mode = mode,
                        apps = state.apps,
                        favorites = favorites,
                        searchQuery = searchQuery,
                        onSearchQueryChange = onSearchQueryChange,
                        viewMode = viewMode,
                        onViewModeChange = onViewModeChange,
                        category = category,
                        onCategoryChange = onCategoryChange,
                        draft = draft,
                        onDraftChange = { draft = it },
                        onToggleFavorite = onToggleFavorite,
                        onSetFavorites = onSetFavorites,
                        onLaunch = handleLaunch,
                        onRefresh = onRefresh,
                        loadIcon = loadIcon,
                        onOpenHome = onOpenHome,
                        onOpenNavigation = onOpenNavigation,
                        onOpenDiagnostics = onOpenDiagnostics,
                        onOpenSettings = onOpenSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.DrawerBody(
    mode: AppDrawerMode,
    apps: List<InstalledApp>,
    favorites: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewMode: AppDrawerView,
    onViewModeChange: (AppDrawerView) -> Unit,
    category: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    draft: List<String>?,
    onDraftChange: (List<String>?) -> Unit,
    onToggleFavorite: (InstalledApp) -> Unit,
    onSetFavorites: (List<String>) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    onRefresh: () -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    onOpenHome: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val editing = draft != null
    val activeFavorites = draft ?: favorites
    val favoriteApps = remember(apps, activeFavorites) {
        AppCatalog.resolveFavorites(activeFavorites, apps)
    }
    val newlyInstalled = remember(apps) {
        AppCatalog.newlyInstalled(apps, NEWLY_INSTALLED_LIMIT)
    }
    val draftKeys = remember(draft) { draft?.toSet() }
    val storedKeys = remember(favorites) { favorites.toSet() }
    val favoriteKeys = draftKeys ?: storedKeys

    // While editing, every star toggle anywhere in the drawer stages the
    // change in the draft instead of persisting immediately.
    val toggle: (InstalledApp) -> Unit = { app ->
        val current = draft
        if (current != null) {
            onDraftChange(
                if (app.key in current) current - app.key else current + app.key,
            )
        } else {
            onToggleFavorite(app)
        }
    }
    val startEditing: () -> Unit = {
        onDraftChange(AppCatalog.pruneFavorites(favorites, apps))
    }
    val cancelEditing: () -> Unit = { onDraftChange(null) }
    val saveEditing: () -> Unit = {
        val current = draft
        if (current != null) {
            onSetFavorites(current)
            onDraftChange(null)
        }
    }

    val showSections = mode != AppDrawerMode.EXPANDED &&
        searchQuery.isBlank() &&
        category == AppCategory.ALL

    when (mode) {
        AppDrawerMode.EXPANDED -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
        ) {
            AppNavigationRail(
                selected = RailDestination.APPS,
                onOpenHome = onOpenHome,
                onOpenAppDrawer = {},
                onOpenNavigation = onOpenNavigation,
                onOpenDiagnostics = onOpenDiagnostics,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .width(AutoVerseSpacing.railWidth)
                    .fillMaxHeight(),
            )
            CentralPanel(
                apps = apps,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
                category = category,
                onCategoryChange = onCategoryChange,
                favoriteKeys = favoriteKeys,
                onToggle = toggle,
                onLaunch = onLaunch,
                loadIcon = loadIcon,
                onRefresh = onRefresh,
                showSections = false,
                favoriteApps = favoriteApps,
                newlyInstalled = newlyInstalled,
                editing = editing,
                canEdit = favorites.isNotEmpty(),
                onStartEdit = startEditing,
                onCancelEdit = cancelEditing,
                onSaveEdit = saveEditing,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .width(RIGHT_COLUMN_WIDTH)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
            ) {
                FavoritesCard(
                    favoriteApps = favoriteApps,
                    editing = editing,
                    canEdit = favorites.isNotEmpty(),
                    onStartEdit = startEditing,
                    onCancelEdit = cancelEditing,
                    onSaveEdit = saveEditing,
                    onToggle = toggle,
                    onLaunch = onLaunch,
                    loadIcon = loadIcon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                NewlyInstalledCard(
                    apps = newlyInstalled,
                    favoriteKeys = favoriteKeys,
                    onToggle = toggle,
                    onLaunch = onLaunch,
                    loadIcon = loadIcon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
        AppDrawerMode.MEDIUM -> Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
        ) {
            AppNavigationRail(
                selected = RailDestination.APPS,
                onOpenHome = onOpenHome,
                onOpenAppDrawer = {},
                onOpenNavigation = onOpenNavigation,
                onOpenDiagnostics = onOpenDiagnostics,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .width(AutoVerseSpacing.railWidth)
                    .fillMaxHeight(),
            )
            CentralPanel(
                apps = apps,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                viewMode = viewMode,
                onViewModeChange = onViewModeChange,
                category = category,
                onCategoryChange = onCategoryChange,
                favoriteKeys = favoriteKeys,
                onToggle = toggle,
                onLaunch = onLaunch,
                loadIcon = loadIcon,
                onRefresh = onRefresh,
                showSections = showSections,
                favoriteApps = favoriteApps,
                newlyInstalled = newlyInstalled,
                editing = editing,
                canEdit = favorites.isNotEmpty(),
                onStartEdit = startEditing,
                onCancelEdit = cancelEditing,
                onSaveEdit = saveEditing,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
        AppDrawerMode.COMPACT -> CentralPanel(
            apps = apps,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            viewMode = viewMode,
            onViewModeChange = onViewModeChange,
            category = category,
            onCategoryChange = onCategoryChange,
            favoriteKeys = favoriteKeys,
            onToggle = toggle,
            onLaunch = onLaunch,
            loadIcon = loadIcon,
            onRefresh = onRefresh,
            showSections = showSections,
            favoriteApps = favoriteApps,
            newlyInstalled = newlyInstalled,
            editing = editing,
            canEdit = favorites.isNotEmpty(),
            onStartEdit = startEditing,
            onCancelEdit = cancelEditing,
            onSaveEdit = saveEditing,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun CentralPanel(
    apps: List<InstalledApp>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    viewMode: AppDrawerView,
    onViewModeChange: (AppDrawerView) -> Unit,
    category: AppCategory,
    onCategoryChange: (AppCategory) -> Unit,
    favoriteKeys: Set<String>,
    onToggle: (InstalledApp) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    onRefresh: () -> Unit,
    showSections: Boolean,
    favoriteApps: List<InstalledApp>,
    newlyInstalled: List<InstalledApp>,
    editing: Boolean,
    canEdit: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CockpitCard(
        modifier = modifier,
        contentPadding = PaddingValues(AutoVerseSpacing.md),
    ) {
        if (apps.isEmpty()) {
            DrawerEmpty(
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            )
            return@CockpitCard
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
        ) {
            SearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
            )
            ViewModeToggle(
                icon = Icons.Outlined.GridView,
                contentDescription = "Grid view",
                selected = viewMode == AppDrawerView.GRID,
                onSelect = { onViewModeChange(AppDrawerView.GRID) },
            )
            ViewModeToggle(
                icon = Icons.AutoMirrored.Outlined.ViewList,
                contentDescription = "List view",
                selected = viewMode == AppDrawerView.LIST,
                onSelect = { onViewModeChange(AppDrawerView.LIST) },
            )
        }
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        CategoryChips(selected = category, onSelect = onCategoryChange)
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        val filtered = remember(apps, searchQuery, category) {
            AppCatalog.filter(apps, searchQuery, category)
        }
        if (filtered.isEmpty()) {
            val colors = LocalAutoVerseColors.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "No apps match \u201C${searchQuery.trim()}\u201D"
                    } else {
                        "No apps in ${category.label}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            return@CockpitCard
        }
        when (viewMode) {
            AppDrawerView.GRID -> AppGrid(
                filtered = filtered,
                favoriteKeys = favoriteKeys,
                favoriteApps = favoriteApps,
                newlyInstalled = newlyInstalled,
                showSections = showSections,
                editing = editing,
                canEdit = canEdit,
                onStartEdit = onStartEdit,
                onCancelEdit = onCancelEdit,
                onSaveEdit = onSaveEdit,
                onToggle = onToggle,
                onLaunch = onLaunch,
                loadIcon = loadIcon,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            AppDrawerView.LIST -> AppList(
                filtered = filtered,
                favoriteKeys = favoriteKeys,
                favoriteApps = favoriteApps,
                newlyInstalled = newlyInstalled,
                showSections = showSections,
                editing = editing,
                canEdit = canEdit,
                onStartEdit = onStartEdit,
                onCancelEdit = onCancelEdit,
                onSaveEdit = onSaveEdit,
                onToggle = onToggle,
                onLaunch = onLaunch,
                loadIcon = loadIcon,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun ViewModeToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    IconToggleButton(
        checked = selected,
        onCheckedChange = { checked -> if (checked) onSelect() },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(AutoVerseIcons.standard),
            tint = if (selected) colors.accent else colors.textFaint,
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
            CategoryChip(
                label = entry.label,
                selected = entry == selected,
                onClick = { onSelect(entry) },
            )
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
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
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
private fun AppGrid(
    filtered: List<InstalledApp>,
    favoriteKeys: Set<String>,
    favoriteApps: List<InstalledApp>,
    newlyInstalled: List<InstalledApp>,
    showSections: Boolean,
    editing: Boolean,
    canEdit: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onToggle: (InstalledApp) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 124.dp),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
    ) {
        if (showSections) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "section-favorites-header") {
                FavoritesHeader(
                    editing = editing,
                    canEdit = canEdit,
                    onStartEdit = onStartEdit,
                    onCancelEdit = onCancelEdit,
                    onSaveEdit = onSaveEdit,
                )
            }
            if (favoriteApps.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "section-favorites-empty") {
                    SectionMessage("No favorites yet. Tap the star on any app.")
                }
            } else {
                items(favoriteApps, key = { "fav-${it.key}" }) { app ->
                    AppGridTile(
                        app = app,
                        isFavorite = true,
                        onToggle = onToggle,
                        onLaunch = onLaunch,
                        loadIcon = loadIcon,
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "section-new-header") {
                SectionLabel(icon = Icons.Outlined.NewReleases, label = "NEWLY INSTALLED")
            }
            if (newlyInstalled.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "section-new-empty") {
                    SectionMessage("Apps you install will appear here.")
                }
            } else {
                items(newlyInstalled, key = { "new-${it.key}" }) { app ->
                    AppGridTile(
                        app = app,
                        isFavorite = app.key in favoriteKeys,
                        onToggle = onToggle,
                        onLaunch = onLaunch,
                        loadIcon = loadIcon,
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "section-all-header") {
                SectionLabel(icon = Icons.Outlined.Apps, label = "ALL APPS")
            }
        }
        items(filtered, key = { "app-${it.key}" }) { app ->
            AppGridTile(
                app = app,
                isFavorite = app.key in favoriteKeys,
                onToggle = onToggle,
                onLaunch = onLaunch,
                loadIcon = loadIcon,
            )
        }
    }
}

@Composable
private fun AppList(
    filtered: List<InstalledApp>,
    favoriteKeys: Set<String>,
    favoriteApps: List<InstalledApp>,
    newlyInstalled: List<InstalledApp>,
    showSections: Boolean,
    editing: Boolean,
    canEdit: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onToggle: (InstalledApp) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        if (showSections) {
            item(key = "section-favorites-header") {
                FavoritesHeader(
                    editing = editing,
                    canEdit = canEdit,
                    onStartEdit = onStartEdit,
                    onCancelEdit = onCancelEdit,
                    onSaveEdit = onSaveEdit,
                )
            }
            if (favoriteApps.isEmpty()) {
                item(key = "section-favorites-empty") {
                    SectionMessage("No favorites yet. Tap the star on any app.")
                }
            } else {
                items(favoriteApps, key = { "fav-${it.key}" }) { app ->
                    AppListEntry(
                        app = app,
                        isFavorite = true,
                        onToggle = onToggle,
                        onLaunch = onLaunch,
                        loadIcon = loadIcon,
                    )
                }
            }
            item(key = "section-new-header") {
                SectionLabel(icon = Icons.Outlined.NewReleases, label = "NEWLY INSTALLED")
            }
            if (newlyInstalled.isEmpty()) {
                item(key = "section-new-empty") {
                    SectionMessage("Apps you install will appear here.")
                }
            } else {
                items(newlyInstalled, key = { "new-${it.key}" }) { app ->
                    AppListEntry(
                        app = app,
                        isFavorite = app.key in favoriteKeys,
                        onToggle = onToggle,
                        onLaunch = onLaunch,
                        loadIcon = loadIcon,
                    )
                }
            }
            item(key = "section-all-header") {
                SectionLabel(icon = Icons.Outlined.Apps, label = "ALL APPS")
            }
        }
        items(filtered, key = { "app-${it.key}" }) { app ->
            AppListEntry(
                app = app,
                isFavorite = app.key in favoriteKeys,
                onToggle = onToggle,
                onLaunch = onLaunch,
                loadIcon = loadIcon,
            )
        }
    }
}

@Composable
private fun FavoritesHeader(
    editing: Boolean,
    canEdit: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        SectionLabel(
            icon = Icons.Filled.Star,
            label = "FAVORITES",
            modifier = Modifier.weight(1f),
        )
        if (editing) {
            TextAction(text = "Cancel", onClick = onCancelEdit)
            TextAction(text = "Save", onClick = onSaveEdit)
        } else if (canEdit) {
            TextAction(text = "Edit", onClick = onStartEdit)
        }
    }
}

@Composable
private fun FavoritesCard(
    favoriteApps: List<InstalledApp>,
    editing: Boolean,
    canEdit: Boolean,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onToggle: (InstalledApp) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    CockpitCard(
        modifier = modifier,
        contentPadding = PaddingValues(AutoVerseSpacing.md),
    ) {
        FavoritesHeader(
            editing = editing,
            canEdit = canEdit,
            onStartEdit = onStartEdit,
            onCancelEdit = onCancelEdit,
            onSaveEdit = onSaveEdit,
        )
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        if (favoriteApps.isEmpty()) {
            SectionMessage("No favorites yet. Tap the star on any app.")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                favoriteApps.forEach { app ->
                    key(app.key) {
                        AppListEntry(
                            app = app,
                            isFavorite = true,
                            onToggle = onToggle,
                            onLaunch = onLaunch,
                            loadIcon = loadIcon,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewlyInstalledCard(
    apps: List<InstalledApp>,
    favoriteKeys: Set<String>,
    onToggle: (InstalledApp) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    CockpitCard(
        modifier = modifier,
        contentPadding = PaddingValues(AutoVerseSpacing.md),
    ) {
        SectionLabel(icon = Icons.Outlined.NewReleases, label = "NEWLY INSTALLED")
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        if (apps.isEmpty()) {
            SectionMessage("Apps you install will appear here.")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                apps.forEach { app ->
                    key(app.key) {
                        AppListEntry(
                            app = app,
                            isFavorite = app.key in favoriteKeys,
                            onToggle = onToggle,
                            onLaunch = onLaunch,
                            loadIcon = loadIcon,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppGridTile(
    app: InstalledApp,
    isFavorite: Boolean,
    onToggle: (InstalledApp) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, app.key) {
        value = loadIcon(app)
    }
    AppTile(
        label = app.label,
        icon = icon,
        isFavorite = isFavorite,
        onToggleFavorite = { onToggle(app) },
        onClick = { onLaunch(app) },
    )
}

@Composable
private fun AppListEntry(
    app: InstalledApp,
    isFavorite: Boolean,
    onToggle: (InstalledApp) -> Unit,
    onLaunch: (InstalledApp) -> Unit,
    loadIcon: suspend (InstalledApp) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val icon by produceState<ImageBitmap?>(initialValue = null, app.key) {
        value = loadIcon(app)
    }
    AppListRow(
        label = app.label,
        icon = icon,
        isFavorite = isFavorite,
        onToggleFavorite = { onToggle(app) },
        onClick = { onLaunch(app) },
        modifier = modifier,
    )
}

@Composable
private fun SectionMessage(text: String, modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.textSecondary,
        modifier = modifier.padding(vertical = AutoVerseSpacing.xxs),
    )
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
        placeholder = {
            Text(
                text = "Search apps",
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

@Composable
private fun DrawerLoading(modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
        ) {
            CircularProgressIndicator(color = colors.accent)
            Text(
                text = "Loading apps\u2026",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
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
                text = "Couldn\u2019t load apps",
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
            Icon(
                Icons.Outlined.Apps,
                contentDescription = null,
                modifier = Modifier.size(AutoVerseIcons.prominent),
                tint = colors.textFaint,
            )
            Text(
                text = "No launchable apps found",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = "Apps that declare a launcher activity will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            PrimaryAction(text = "Refresh", onClick = onRefresh)
        }
    }
}
