package com.autoverse.launcher.feature.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.navigation.RecentDestination
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.navigation.SavedPlace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Phase 18 — navigation page behavior pins. The embedded map surface is
 * injected as a stub, so these tests exercise the real page layout, search
 * states, suggestions, the places panel reflow, and the handoff paths
 * without MapLibre native libraries.
 *
 * Sizes default to a phone-sized window so every asserted node is on-screen
 * and clickable; the WIDE head-unit layout is driven through
 * [Modifier.requiredSize] beyond the test window, so its structure is
 * asserted with existence checks (same discipline as AppDrawerScreenTest).
 */
class NavigationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val maps = ResolvedNavigationProvider(
        provider = NavigationProvider.GoogleMaps,
        preferredMissing = false,
    )

    private fun setScreen(
        widthDp: Int = 400,
        heightDp: Int = 800,
        resolved: ResolvedNavigationProvider = maps,
        savedPlaces: List<SavedPlace> = emptyList(),
        recentDestinations: List<RecentDestination> = emptyList(),
        embeddedMapEnabled: Boolean = false,
        initialQuery: String = "",
        placesPanelVisible: Boolean = true,
        onNavigate: suspend (NavigationDestination) -> NavigationOutcome = {
            NavigationOutcome.OPENED
        },
        onOpenNavigationApp: suspend () -> NavigationOutcome = { NavigationOutcome.OPENED },
        onSavePlace: (String, NavigationDestination) -> Unit = { _, _ -> },
        onRemovePlace: (SavedPlace) -> Unit = {},
        onChangeProvider: () -> Unit = {},
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                var query by remember { mutableStateOf(initialQuery) }
                var panelVisible by remember { mutableStateOf(placesPanelVisible) }
                Box(Modifier.requiredSize(widthDp.dp, heightDp.dp)) {
                    NavigationScreen(
                        resolved = resolved,
                        savedPlaces = savedPlaces,
                        recentDestinations = recentDestinations,
                        embeddedMapEnabled = embeddedMapEnabled,
                        query = query,
                        onQueryChange = { query = it },
                        placesPanelVisible = panelVisible,
                        onTogglePlacesPanel = { panelVisible = !panelVisible },
                        onNavigate = onNavigate,
                        onOpenNavigationApp = onOpenNavigationApp,
                        onSavePlace = onSavePlace,
                        onRemovePlace = onRemovePlace,
                        onChangeProvider = onChangeProvider,
                        onBack = {},
                        embeddedMapSurface = if (embeddedMapEnabled) {
                            { Box(Modifier.fillMaxSize().testTag("map-surface")) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    @Test
    fun disabledEmbeddedMapShowsTheHandoffSurface() {
        setScreen()
        composeRule.onNodeWithText("Turn-by-turn with Google Maps").assertExists()
        composeRule.onNodeWithText("Open Google Maps").assertExists()
        composeRule.onNodeWithTag("map-surface").assertDoesNotExist()
    }

    @Test
    fun embeddedMapWhenEnabledUsesTheInjectedSurface() {
        setScreen(embeddedMapEnabled = true)
        composeRule.onNodeWithTag("map-surface").assertExists()
        composeRule.onNodeWithText("Turn-by-turn with Google Maps").assertDoesNotExist()
    }

    @Test
    fun navigateIsDisabledUntilTheDestinationIsValid() {
        setScreen()
        composeRule.onNodeWithText("Navigate").assertIsNotEnabled()
    }

    @Test
    fun navigateHandsOffTheTypedDestination() {
        val handedOff = AtomicReference<NavigationDestination?>(null)
        setScreen(initialQuery = "Vienna", onNavigate = { destination ->
            handedOff.set(destination)
            NavigationOutcome.OPENED
        })
        composeRule.onNodeWithText("Navigate").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { handedOff.get() != null }
        assertEquals(NavigationDestination.Query("Vienna"), handedOff.get())
    }

    @Test
    fun invalidCoordinatesShowFailureCopy() {
        setScreen(initialQuery = "95, 200")
        composeRule.onNodeWithText(
            "Latitude must be -90 to 90 and longitude -180 to 180",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertIsNotEnabled()
    }

    @Test
    fun clearActionEmptiesTheQuery() {
        setScreen(initialQuery = "Vienna")
        composeRule.onNodeWithText("Navigate").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear destination").performClick()
        composeRule.onNodeWithText("Where to?").assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertIsNotEnabled()
    }

    @Test
    fun savedPlaceSuggestionsFillTheQueryWhenTapped() {
        setScreen(
            initialQuery = "prat",
            placesPanelVisible = false,
            savedPlaces = listOf(
                SavedPlace("Home", NavigationDestination.Query("Praterstern")),
            ),
        )
        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("Praterstern").assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertIsDisplayed()
    }

    @Test
    fun recentsOfferSuggestions() {
        setScreen(
            initialQuery = "airport",
            placesPanelVisible = false,
            recentDestinations = listOf(
                RecentDestination(NavigationDestination.Query("Vienna Airport")),
            ),
        )
        composeRule.onNodeWithText("Vienna Airport").assertIsDisplayed()
    }

    @Test
    fun noLocalMatchesExplainsTheHandoff() {
        setScreen(initialQuery = "xyzzy")
        composeRule.onNodeWithText(
            "No local matches \u2014 your text is handed off as a search.",
        ).assertIsDisplayed()
    }

    @Test
    fun panelToggleHidesAndShowsThePlacesPanel() {
        setScreen(
            savedPlaces = listOf(
                SavedPlace("Home", NavigationDestination.Query("Praterstern")),
            ),
        )
        composeRule.onNodeWithText("PLACES").assertIsDisplayed()
        composeRule.onNodeWithText("Hide places panel").performClick()
        composeRule.onNodeWithText("PLACES").assertDoesNotExist()
        composeRule.onNodeWithText("Show places panel").assertIsDisplayed()
        composeRule.onNodeWithText("Show places panel").performClick()
        composeRule.onNodeWithText("PLACES").assertIsDisplayed()
    }

    @Test
    fun hidingThePanelKeepsTheMapSurfaceAlive() {
        setScreen(embeddedMapEnabled = true)
        composeRule.onNodeWithTag("map-surface").assertExists()
        composeRule.onNodeWithText("Hide places panel").performClick()
        composeRule.onNodeWithTag("map-surface").assertExists()
    }

    @Test
    fun narrowWindowsStackBothSurfaces() {
        setScreen(widthDp = 400, heightDp = 800)
        composeRule.onNodeWithText("NAVIGATION").assertIsDisplayed()
        composeRule.onNodeWithText("PLACES").assertIsDisplayed()
    }

    @Test
    fun wideWindowsShowTheSidePanelAlongsideTheMap() {
        setScreen(widthDp = 1100, heightDp = 700, embeddedMapEnabled = true)
        composeRule.onNodeWithText("PLACES").assertExists()
        composeRule.onNodeWithTag("map-surface").assertExists()
    }

    @Test
    fun savingAUsesTheLabelAndClearsTheForm() {
        val saved = AtomicReference<Pair<String, NavigationDestination>?>(null)
        setScreen(
            initialQuery = "Vienna",
            onSavePlace = { label, destination -> saved.set(label to destination) },
        )
        composeRule.onNodeWithText("Label (optional)").performTextInput("Home")
        composeRule.onNodeWithText("Save").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitUntil(timeoutMillis = 5_000) { saved.get() != null }
        assertEquals("Home" to NavigationDestination.Query("Vienna"), saved.get())
        composeRule.onNodeWithText("Label (optional)").assertExists()
    }

    @Test
    fun removingASavedPlaceInvokesTheCallback() {
        val removed = AtomicReference<SavedPlace?>(null)
        val home = SavedPlace("Home", NavigationDestination.Query("Praterstern"))
        setScreen(savedPlaces = listOf(home), onRemovePlace = { removed.set(it) })
        composeRule.onNodeWithContentDescription("Remove Home")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(home, removed.get())
    }

    @Test
    fun handoffFailuresSurfaceTheProviderMessage() {
        setScreen(
            initialQuery = "Vienna",
            onNavigate = { NavigationOutcome.NO_PROVIDER },
        )
        composeRule.onNodeWithText("Navigate").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("No navigation app is available")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun aStalePreferredAppIsAdvertisedAsMissing() {
        setScreen(
            resolved = ResolvedNavigationProvider(
                provider = NavigationProvider.GoogleMaps,
                preferredMissing = true,
            ),
        )
        composeRule.onNodeWithText(
            "Preferred app isn't installed \u2014 using Google Maps",
        ).assertIsDisplayed()
    }

    @Test
    fun openOnlyProvidersExplainTheirOwnRouting() {
        setScreen(
            resolved = ResolvedNavigationProvider(
                provider = NavigationProvider.OpenOnly("com.example.nav", "Example Nav"),
                preferredMissing = false,
            ),
        )
        composeRule.onNodeWithText(
            "This app plans routes inside its own app. Open Example Nav to enter a " +
                "destination there.",
        ).assertIsDisplayed()
        composeRule.onAllNodesWithText("Open Example Nav").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertDoesNotExist()
    }

    @Test
    fun emptyPlacesPanelShowsHonestEmptyHints() {
        setScreen()
        composeRule.onNodeWithText(
            "Nothing saved yet. Type a destination above, then tap Save.",
        ).assertExists()
        composeRule.onNodeWithText("Destinations you hand off will appear here.")
            .assertExists()
    }

    @Test
    fun recentDestinationsOfferNoDuplicatesWithSavedPlaces() {
        setScreen(
            initialQuery = "prater",
            placesPanelVisible = false,
            savedPlaces = listOf(
                SavedPlace("Home", NavigationDestination.Query("Praterstern")),
            ),
            recentDestinations = listOf(
                RecentDestination(NavigationDestination.Query("Praterstern")),
            ),
        )
        composeRule.onAllNodesWithText("Praterstern").assertCountEquals(1)
    }
}
