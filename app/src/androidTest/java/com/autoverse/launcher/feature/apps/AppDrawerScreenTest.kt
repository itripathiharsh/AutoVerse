package com.autoverse.launcher.feature.apps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Apps page behavior pins (0.28.2): All Apps beside/collapsed Recently Used.
 * There is no map, favorites section, or view-mode toggle on this screen.
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

    private fun setDrawer(
        state: AppListState,
        recentApps: List<InstalledApp> = emptyList(),
        recentCollapsed: Boolean = false,
        searchQuery: String = "",
        category: AppCategory = AppCategory.ALL,
        widthDp: Int = 0,
        onLaunchApp: suspend (InstalledApp) -> LaunchOutcome = { LaunchOutcome.LAUNCHED },
        onClearRecent: () -> Unit = {},
        onRecentCollapsedChange: (Boolean) -> Unit = {},
        onOpenNavigation: () -> Unit = {},
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                var query by remember { mutableStateOf(searchQuery) }
                var selectedCategory by remember { mutableStateOf(category) }
                var collapsed by remember { mutableStateOf(recentCollapsed) }
                val drawer: @Composable () -> Unit = {
                    AppDrawerScreen(
                        clockText = "09:41",
                        dateText = "Wednesday, 15 October",
                        connectivity = ConnectivityStatus(
                            wifi = LinkState.CONNECTED,
                            bluetooth = LinkState.CONNECTED,
                        ),
                        state = state,
                        searchQuery = query,
                        onSearchQueryChange = { query = it },
                        category = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        onLaunchApp = onLaunchApp,
                        onRefresh = {},
                        loadIcon = { null },
                        onOpenHome = {},
                        onOpenSettings = {},
                        onOpenDiagnostics = {},
                        onOpenNavigation = onOpenNavigation,
                        recentApps = recentApps,
                        recentCollapsed = collapsed,
                        onRecentCollapsedChange = { collapsed = it },
                        onClearRecent = onClearRecent,
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
    fun emptyStateShowsGuidance() {
        setDrawer(AppListState.Ready(emptyList()))
        composeRule.onNodeWithText("No launchable apps found").assertIsDisplayed()
    }

    @Test
    fun readyStateListsApps() {
        setDrawer(AppListState.Ready(apps))
        composeRule.onNodeWithText("Bravo Maps").assertIsDisplayed()
        composeRule.onNodeWithText("Charlie Phone").assertIsDisplayed()
        composeRule.onNodeWithText("ALL APPS \u00b7 2").assertIsDisplayed()
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

    // --- recently used ------------------------------------------------------

    @Test
    fun recentlyUsedSectionShowsEntriesAndClearAction() {
        val cleared = AtomicReference(false)
        setDrawer(
            state = AppListState.Ready(apps),
            recentApps = listOf(apps[1]),
            onClearRecent = { cleared.set(true) },
        )
        composeRule.onNodeWithText("RECENTLY USED").assertIsDisplayed()
        composeRule.onAllNodesWithText("Charlie Phone").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Clear").performClick()
        assertEquals(true, cleared.get())
    }

    @Test
    fun emptyHistoryShowsTheHonestMessage() {
        setDrawer(state = AppListState.Ready(apps))
        composeRule.onNodeWithText("Apps opened from AutoVerse").assertIsDisplayed()
    }

    @Test
    fun collapsingThePanelExpandsAllAppsAndKeepsARestoreControl() {
        var changed: Boolean? = null
        setDrawer(
            state = AppListState.Ready(apps),
            recentApps = listOf(apps[0]),
            onRecentCollapsedChange = { changed = it },
        )
        composeRule.onNodeWithContentDescription("Hide recently used").performClick()
        assertEquals(true, changed)

        setDrawer(
            state = AppListState.Ready(apps),
            recentApps = listOf(apps[0]),
            recentCollapsed = true,
        )
        composeRule.onNodeWithContentDescription("Show recently used").assertIsDisplayed()
    }

    // --- adaptive layouts ---------------------------------------------------

    @Test
    fun wideLayoutShowsBothPanelsSideBySide() {
        setDrawer(
            state = AppListState.Ready(apps),
            recentApps = listOf(apps[0]),
            widthDp = 1200,
        )
        composeRule.onNodeWithText("ALL APPS \u00b7 2").assertExists()
        composeRule.onNodeWithText("RECENTLY USED").assertExists()
        composeRule.onNodeWithText("Home").assertExists()
    }

    @Test
    fun railNavigationInvokesOpenNavigationCallback() {
        val invoked = AtomicReference(false)
        setDrawer(
            state = AppListState.Ready(apps),
            widthDp = 1200,
            onOpenNavigation = { invoked.set(true) },
        )
        composeRule.onAllNodesWithText("Maps").onFirst().performClick()
        assertTrue(invoked.get())
    }
}
