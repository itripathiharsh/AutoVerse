package com.autoverse.launcher.feature.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.connectivity.LinkState
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.launcher.LaunchOutcome
import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.InstalledApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Phase 15 — drawer behavior pins. Layouts taller/wider than the test window
 * (MEDIUM, EXPANDED) are driven through [Modifier.requiredSize] and asserted
 * with existence checks, because the surrounding window still clips anything
 * beyond its bounds; interactions on off-screen nodes go through the
 * semantics onClick action rather than touch injection.
 */
class AppDrawerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val apps = listOf(
        InstalledApp(
            packageName = "com.bravo.maps",
            activityName = "com.bravo.maps.MainActivity",
            label = "Bravo Maps",
            category = AppCategory.NAVIGATION,
        ),
        InstalledApp(
            packageName = "com.charlie.phone",
            activityName = "com.charlie.phone.MainActivity",
            label = "Charlie Phone",
            category = AppCategory.COMMUNICATION,
        ),
    )

    private val appsWithInstallTimes = apps + InstalledApp(
        packageName = "com.delta.player",
        activityName = "com.delta.player.MainActivity",
        label = "Delta Player",
        category = AppCategory.MUSIC_AUDIO,
        firstInstallTime = 1_700_000_000_000L,
    )

    private fun setDrawer(
        state: AppListState,
        favorites: List<String> = emptyList(),
        searchQuery: String = "",
        category: AppCategory = AppCategory.ALL,
        widthDp: Int = 0,
        onToggleFavorite: (InstalledApp) -> Unit = {},
        onSetFavorites: (List<String>) -> Unit = {},
        onLaunchApp: suspend (InstalledApp) -> LaunchOutcome = { LaunchOutcome.LAUNCHED },
        onRefresh: () -> Unit = {},
        onOpenNavigation: () -> Unit = {},
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                var viewMode by remember { mutableStateOf(AppDrawerView.GRID) }
                var selectedCategory by remember { mutableStateOf(category) }
                var query by remember { mutableStateOf(searchQuery) }
                val drawer: @Composable () -> Unit = {
                    AppDrawerScreen(
                        clockText = "09:41",
                        dateText = "Wednesday, 15 October",
                        connectivity = ConnectivityStatus(
                            wifi = LinkState.CONNECTED,
                            bluetooth = LinkState.CONNECTED,
                        ),
                        state = state,
                        favorites = favorites,
                        searchQuery = query,
                        onSearchQueryChange = { query = it },
                        viewMode = viewMode,
                        onViewModeChange = { viewMode = it },
                        category = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        onToggleFavorite = onToggleFavorite,
                        onSetFavorites = onSetFavorites,
                        onLaunchApp = onLaunchApp,
                        onRefresh = onRefresh,
                        loadIcon = { null },
                        onBack = {},
                        onOpenHome = {},
                        onOpenSettings = {},
                        onOpenDiagnostics = {},
                        onOpenNavigation = onOpenNavigation,
                    )
                }
                if (widthDp > 0) {
                    Box(Modifier.requiredSize(widthDp.dp, 800.dp)) { drawer() }
                } else {
                    drawer()
                }
            }
        }
    }

    // --- state coverage -----------------------------------------------------

    @Test
    fun loadingStateShowsProgress() {
        setDrawer(AppListState.Loading)
        composeRule.onNodeWithText("Loading apps\u2026").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndRetryFires() {
        val refreshRequested = AtomicReference(false)
        setDrawer(
            state = AppListState.Error("Package manager unavailable"),
            onRefresh = { refreshRequested.set(true) },
        )
        composeRule.onNodeWithText("Couldn\u2019t load apps").assertIsDisplayed()
        composeRule.onNodeWithText("Package manager unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertEquals(true, refreshRequested.get())
    }

    @Test
    fun emptyStateShowsGuidance() {
        setDrawer(AppListState.Ready(emptyList()))
        composeRule.onNodeWithText("No launchable apps found").assertIsDisplayed()
    }

    @Test
    fun readyStateListsAppsAndStarTogglesFavorite() {
        val toggled = AtomicReference<InstalledApp?>(null)
        setDrawer(
            state = AppListState.Ready(apps),
            onToggleFavorite = { toggled.set(it) },
        )
        composeRule.onNodeWithText("Bravo Maps").assertIsDisplayed()
        composeRule.onNodeWithText("Charlie Phone").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Add Bravo Maps to favorites")
            .performClick()
        assertEquals("com.bravo.maps", toggled.get()?.packageName)
    }

    @Test
    fun searchQueryFiltersTheGrid() {
        setDrawer(
            state = AppListState.Ready(apps),
            searchQuery = "bravo",
        )
        composeRule.onNodeWithText("Bravo Maps").assertIsDisplayed()
        composeRule.onNodeWithText("Charlie Phone").assertDoesNotExist()
    }

    @Test
    fun categoryChipFiltersTheGrid() {
        setDrawer(state = AppListState.Ready(apps))
        composeRule.onNodeWithText("Bravo Maps").assertIsDisplayed()
        composeRule.onNodeWithText("Navigation").performClick()
        composeRule.onNodeWithText("Bravo Maps").assertIsDisplayed()
        composeRule.onNodeWithText("Charlie Phone").assertDoesNotExist()
    }

    @Test
    fun viewModeTogglesSwitchBetweenGridAndList() {
        setDrawer(state = AppListState.Ready(apps))
        composeRule.onNodeWithContentDescription("Grid view").assertIsOn()
        composeRule.onNodeWithContentDescription("List view").performClick()
        composeRule.onNodeWithContentDescription("List view").assertIsOn()
        composeRule.onNodeWithText("Bravo Maps").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Grid view").performClick()
        composeRule.onNodeWithContentDescription("Grid view").assertIsOn()
    }

    @Test
    fun failedLaunchShowsSnackbarWithRecoveryMessage() {
        setDrawer(
            state = AppListState.Ready(apps),
            onLaunchApp = { LaunchOutcome.NOT_FOUND },
        )
        composeRule.onNodeWithText("Bravo Maps").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("Bravo Maps is no longer installed")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // --- sections -----------------------------------------------------------

    @Test
    fun favoritesSectionShowsHeadersAndEditAffordance() {
        setDrawer(
            state = AppListState.Ready(apps),
            favorites = listOf("com.bravo.maps/com.bravo.maps.MainActivity"),
        )
        composeRule.onNodeWithText("FAVORITES").assertIsDisplayed()
        composeRule.onNodeWithText("ALL APPS").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertIsDisplayed()
    }

    @Test
    fun noFavoritesHidesEditAffordance() {
        setDrawer(state = AppListState.Ready(apps))
        composeRule.onNodeWithText("FAVORITES").assertIsDisplayed()
        composeRule.onNodeWithText("Edit").assertDoesNotExist()
    }

    @Test
    fun newlyInstalledSectionShowsMostRecentInstall() {
        setDrawer(state = AppListState.Ready(appsWithInstallTimes))
        composeRule.onNodeWithText("NEWLY INSTALLED").assertIsDisplayed()
        composeRule.onNodeWithText("Apps you install will appear here.").assertDoesNotExist()
        composeRule.onAllNodesWithText("Delta Player").assertCountEquals(2)
    }

    // --- adaptive layouts ---------------------------------------------------

    @Test
    fun mediumLayoutShowsRailAndSections() {
        setDrawer(
            state = AppListState.Ready(apps),
            favorites = listOf("com.bravo.maps/com.bravo.maps.MainActivity"),
            widthDp = 700,
        )
        composeRule.onNodeWithText("Apps").assertIsDisplayed()
        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("FAVORITES").assertIsDisplayed()
        composeRule.onNodeWithText("ALL APPS").assertIsDisplayed()
    }

    @Test
    fun mediumRailNavigationInvokesOpenNavigationCallback() {
        val invoked = AtomicReference(false)
        setDrawer(
            state = AppListState.Ready(apps),
            widthDp = 700,
            onOpenNavigation = { invoked.set(true) },
        )
        composeRule.onAllNodesWithText("Navigation").onFirst().performClick()
        assertEquals(true, invoked.get())
    }

    @Test
    fun expandedLayoutShowsRailAndSidePanels() {
        setDrawer(
            state = AppListState.Ready(appsWithInstallTimes),
            favorites = listOf("com.bravo.maps/com.bravo.maps.MainActivity"),
            widthDp = 1200,
        )
        composeRule.onNodeWithText("Apps").assertExists()
        composeRule.onNodeWithText("FAVORITES").assertExists()
        composeRule.onNodeWithText("NEWLY INSTALLED").assertExists()
        composeRule.onNodeWithText("All Apps").assertExists()
    }

    @Test
    fun expandedEditFlowStagesRemovalAndSaves() {
        val saved = AtomicReference<List<String>?>(null)
        setDrawer(
            state = AppListState.Ready(apps),
            favorites = listOf("com.bravo.maps/com.bravo.maps.MainActivity"),
            widthDp = 1200,
            onSetFavorites = { saved.set(it) },
        )
        composeRule
            .onNodeWithText("Edit")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule
            .onNodeWithText("Save")
            .assertExists()
        composeRule
            .onAllNodesWithContentDescription("Remove Bravo Maps from favorites")
            .onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule
            .onNodeWithText("No favorites yet. Tap the star on any app.")
            .assertExists()
        composeRule
            .onNodeWithText("Save")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(emptyList<String>(), saved.get())
    }
}
