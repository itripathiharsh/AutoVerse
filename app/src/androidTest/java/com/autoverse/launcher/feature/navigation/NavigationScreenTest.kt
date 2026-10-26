package com.autoverse.launcher.feature.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.connectivity.LinkState
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.navigation.MapsStatus
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.navigation.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Maps page behavior pins (0.28.2): the Google Maps launch panel plus the
 * destinations panel (saved + recent), with collapse/restore. No map is ever
 * rendered here; launch outcomes are the typed NavigationOutcome values.
 */
class NavigationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setScreen(
        mapsStatus: MapsStatus = MapsStatus.INSTALLED,
        savedPlaces: List<SavedPlace> = emptyList(),
        recentDestinations: List<RecentDestination> = emptyList(),
        initialQuery: String = "",
        placesCollapsed: Boolean = false,
        widthDp: Int = 0,
        onNavigate: suspend (NavigationDestination) -> NavigationOutcome = {
            NavigationOutcome.REQUESTED
        },
        onOpenGoogleMaps: suspend () -> NavigationOutcome = { NavigationOutcome.REQUESTED },
        onSavePlace: suspend (String, NavigationDestination) -> Boolean = { _, _ -> true },
        onRemovePlace: (SavedPlace) -> Unit = {},
        onClearRecents: () -> Unit = {},
        onPlacesCollapsedChange: (Boolean) -> Unit = {},
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                var query by remember { mutableStateOf(initialQuery) }
                val screen: @Composable () -> Unit = {
                    NavigationScreen(
                        clockText = "09:41",
                        dateText = "Wednesday, 15 October",
                        connectivity = ConnectivityStatus(
                            wifi = LinkState.CONNECTED,
                            bluetooth = LinkState.CONNECTED,
                        ),
                        mapsStatus = mapsStatus,
                        destinationTargetLabel = "Google Maps",
                        savedPlaces = savedPlaces,
                        recentDestinations = recentDestinations,
                        query = query,
                        onQueryChange = { query = it },
                        onNavigate = onNavigate,
                        onOpenGoogleMaps = onOpenGoogleMaps,
                        onSavePlace = onSavePlace,
                        onRemovePlace = onRemovePlace,
                        onClearRecents = onClearRecents,
                        placesCollapsed = placesCollapsed,
                        onPlacesCollapsedChange = onPlacesCollapsedChange,
                        onOpenHome = {},
                    )
                }
                if (widthDp > 0) {
                    Box(Modifier.requiredSize(widthDp.dp, 800.dp)) { screen() }
                } else {
                    screen()
                }
            }
        }
    }

    // --- launch panel -------------------------------------------------------

    @Test
    fun launchPanelNamesMapsAndExplainsItself() {
        setScreen()
        composeRule.onNodeWithText("MAP").assertIsDisplayed()
        composeRule.onNodeWithText("Open Google Maps").assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap contributors © CARTO").assertIsDisplayed()
    }

    @Test
    fun notInstalledStateDisablesTheLaunchAndSaysSo() {
        setScreen(mapsStatus = MapsStatus.NOT_INSTALLED)
        composeRule.onNodeWithText("Google Maps isn\u2019t installed").assertIsDisplayed()
        composeRule.onNodeWithText("Open Google Maps").assertIsNotEnabled()
    }

    @Test
    fun openGoogleMapsInvokesTheLaunchCallback() {
        val opened = AtomicBoolean(false)
        setScreen(onOpenGoogleMaps = {
            opened.set(true)
            NavigationOutcome.REQUESTED
        })
        composeRule.onAllNodesWithText("Open Google Maps").onFirst().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { opened.get() }
        assertTrue(opened.get())
    }

    // --- destination entry --------------------------------------------------

    @Test
    fun navigateIsDisabledUntilTheDestinationIsValid() {
        setScreen()
        composeRule.onNodeWithText("Navigate").assertIsNotEnabled()
    }

    @Test
    fun navigateHandsOffTheTypedDestination() {
        val handedOff = AtomicReference<NavigationDestination?>(null)
        setScreen(
            initialQuery = "Vienna",
            onNavigate = { destination ->
                handedOff.set(destination)
                NavigationOutcome.REQUESTED
            },
        )
        composeRule.onNodeWithText("Navigate").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { handedOff.get() != null }
        assertEquals(NavigationDestination.Query("Vienna"), handedOff.get())
    }

    @Test
    fun invalidCoordinatesShowFailureCopyAndBlockNavigate() {
        setScreen(initialQuery = "95, 200")
        composeRule.onNodeWithText(
            "Latitude must be -90 to 90 and longitude -180 to 180",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertIsNotEnabled()
    }

    // --- destinations panel -------------------------------------------------

    @Test
    fun destinationsPanelShowsSavedAndRecent() {
        setScreen(
            savedPlaces = listOf(SavedPlace("Home", NavigationDestination.Query("Praterstern"))),
            recentDestinations = listOf(
                RecentDestination(NavigationDestination.Query("Vienna Airport")),
            ),
            widthDp = 1200,
        )
        composeRule.onNodeWithText("DESTINATIONS").assertExists()
        composeRule.onNodeWithText("SAVED").assertExists()
        composeRule.onNodeWithText("RECENT").assertExists()
        composeRule.onAllNodesWithText("Home").onFirst().assertExists()
        composeRule.onAllNodesWithText("Vienna Airport").onFirst().assertExists()
    }

    @Test
    fun tappingASavedPlaceFillsTheQuery() {
        setScreen(
            savedPlaces = listOf(SavedPlace("Home", NavigationDestination.Query("Praterstern"))),
            initialQuery = "prat",
            widthDp = 1200,
        )
        composeRule.onAllNodesWithText("Home").onFirst().performClick()
        composeRule.onNodeWithText("Praterstern").assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertIsDisplayed()
    }

    @Test
    fun removingASavedPlaceInvokesTheCallback() {
        val removed = AtomicReference<SavedPlace?>(null)
        val home = SavedPlace("Home", NavigationDestination.Query("Praterstern"))
        setScreen(
            savedPlaces = listOf(home),
            widthDp = 1200,
            onRemovePlace = { removed.set(it) },
        )
        composeRule.onNodeWithContentDescription("Remove Home")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(home, removed.get())
    }

    @Test
    fun savingAPlaceClearsTheFormAndConfirms() {
        val saved = AtomicReference<Pair<String, NavigationDestination>?>(null)
        setScreen(
            initialQuery = "Vienna",
            widthDp = 1200,
            onSavePlace = { label, destination ->
                saved.set(label to destination)
                true
            },
        )
        composeRule.onNodeWithText("Label (optional)").performTextInput("Home")
        composeRule.onNodeWithText("Save").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000) { saved.get() != null }
        assertEquals("Home" to NavigationDestination.Query("Vienna"), saved.get())
        composeRule.onNodeWithText("Saved \u201CHome\u201D").assertExists()
    }

    @Test
    fun recentsCanBeCleared() {
        val cleared = AtomicBoolean(false)
        setScreen(
            recentDestinations = listOf(
                RecentDestination(NavigationDestination.Query("Vienna Airport")),
            ),
            widthDp = 1200,
            onClearRecents = { cleared.set(true) },
        )
        composeRule.onNodeWithText("Clear").performClick()
        assertTrue(cleared.get())
    }

    // --- collapse -----------------------------------------------------------

    @Test
    fun collapseHidesThePanelAndKeepsARestoreControl() {
        var changed: Boolean? = null
        setScreen(
            savedPlaces = listOf(SavedPlace("Home", NavigationDestination.Query("Praterstern"))),
            widthDp = 1200,
            onPlacesCollapsedChange = { changed = it },
        )
        composeRule.onNodeWithContentDescription("Hide destinations")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(true, changed)

        setScreen(
            placesCollapsed = true,
            widthDp = 1200,
            onPlacesCollapsedChange = {},
        )
        composeRule.onNodeWithContentDescription("Show destinations").assertExists()
        composeRule.onNodeWithText("DESTINATIONS").assertDoesNotExist()
    }
}
