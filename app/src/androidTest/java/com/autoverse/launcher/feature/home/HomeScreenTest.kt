package com.autoverse.launcher.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.model.AutoVerseThemeId
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val dashboard = DashboardData(
        clockText = "10:35",
        dateText = "Sat, 17 Oct",
        fpsText = "60 FPS",
    )

    @Test
    fun dashboardPlaceholdersAreDisplayed() {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                HomeScreen(
                    data = dashboard,
                    onOpenSettings = {},
                    onOpenDiagnostics = {},
                )
            }
        }

        composeRule.onNodeWithText("NAVIGATION").assertIsDisplayed()
        composeRule.onNodeWithText("MEDIA").assertIsDisplayed()
        composeRule.onNodeWithText("No media playing").assertIsDisplayed()
        composeRule.onNodeWithText("10:35").assertIsDisplayed()
    }
}
