package com.autoverse.launcher.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.model.AutoVerseThemeId
import org.junit.Rule
import org.junit.Test

/**
 * Home behavior pins (0.28.2): the cockpit is the only Home — the Google
 * Maps launch region, Now Playing, and the speed panel. The old four-card
 * grid no longer exists at any window size.
 */
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dashboard = DashboardData(
        clockText = "10:35",
        dateText = "Sat, 17 Oct",
        fpsText = "60 FPS",
    )

    @Test
    fun cockpitRegionsAreDisplayed() {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                HomeScreen(
                    data = dashboard,
                    onOpenSettings = {},
                    onOpenDiagnostics = {},
                )
            }
        }

        composeRule.onNodeWithText("MAP").assertIsDisplayed()
        composeRule.onNodeWithText("Open Google Maps").assertIsDisplayed()
        composeRule.onNodeWithText("© OpenStreetMap contributors © CARTO").assertIsDisplayed()
        composeRule.onNodeWithText("NOW PLAYING").assertIsDisplayed()
        composeRule.onNodeWithText("No media playing").assertIsDisplayed()
        composeRule.onNodeWithText("10:35").assertIsDisplayed()
    }

    @Test
    fun theOldFourCardGridIsGone() {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                HomeScreen(
                    data = dashboard,
                    onOpenSettings = {},
                    onOpenDiagnostics = {},
                )
            }
        }

        composeRule.onNodeWithText("FAVORITE APPS").assertDoesNotExist()
        composeRule.onNodeWithText("DRIVING STATUS").assertDoesNotExist()
        composeRule.onNodeWithText("NAVIGATION").assertDoesNotExist()
    }
}
