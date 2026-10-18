package com.autoverse.launcher.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.model.AutoVerseThemeId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Phase 7 — MapPocContent renders every prototype state, keeps the injected
 * map surface alive while loading/ready, drops it when the renderer is
 * unavailable, and offers the navigation hand-off fallback exactly when the
 * map cannot render. The map surface is stubbed, so this compiles and runs
 * without MapLibre native libraries.
 */
class MapPocScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setContent(
        state: MapPocState,
        onRetry: () -> Unit = {},
        onOpenNavigationApp: () -> Unit = {},
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                MapPocContent(
                    state = state,
                    providerLabel = "Google Maps",
                    onRetry = onRetry,
                    onOpenNavigationApp = onOpenNavigationApp,
                    mapSurface = { Box(Modifier.fillMaxSize().testTag("map-surface")) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    @Test
    fun loadingShowsProgressOverTheSurface() {
        composeRule.mainClock.autoAdvance = false
        setContent(MapPocState.Loading)
        composeRule.onNodeWithTag("map-surface").assertExists()
        composeRule.onNodeWithText("Loading map\u2026").assertIsDisplayed()
    }

    @Test
    fun readyShowsSurfaceWithoutFallback() {
        setContent(MapPocState.Ready)
        composeRule.onNodeWithTag("map-surface").assertExists()
        composeRule.onNodeWithText("Open in Google Maps").assertDoesNotExist()
        composeRule.onNodeWithText(
            "Map data: MapLibre demo tiles. Prototype only \u2014 " +
                "production needs a licensed tile provider.",
        ).assertIsDisplayed()
    }

    @Test
    fun errorShowsMessageRetryAndFallback() {
        var opens = 0
        setContent(
            MapPocState.Error(MapPocFailure.STYLE_LOAD_FAILED),
            onOpenNavigationApp = { opens++ },
        )
        composeRule.onNodeWithText("Map couldn't load").assertIsDisplayed()
        composeRule.onNodeWithText("Open in Google Maps").performClick()
        assertEquals(1, opens)
        composeRule.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun emptyShowsNoDataMessageAndFallback() {
        setContent(MapPocState.Empty)
        composeRule.onNodeWithText("No map data").assertIsDisplayed()
        composeRule.onNodeWithText("Open in Google Maps").assertIsDisplayed()
    }

    @Test
    fun unavailableHidesSurfaceAndRetryButKeepsFallback() {
        setContent(MapPocState.Unavailable("The map engine couldn't start on this device."))
        composeRule.onNodeWithTag("map-surface").assertDoesNotExist()
        composeRule.onNodeWithText("Map renderer unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Try again").assertDoesNotExist()
        composeRule.onNodeWithText("Open in Google Maps").assertIsDisplayed()
    }

    @Test
    fun retryInvokesCallback() {
        var retries = 0
        setContent(MapPocState.Error(MapPocFailure.LOAD_TIMEOUT), onRetry = { retries++ })
        composeRule.onNodeWithText("Try again").performClick()
        assertEquals(1, retries)
    }
}
