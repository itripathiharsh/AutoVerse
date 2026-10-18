package com.autoverse.launcher.core.design.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.NavigationProvider
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Phase 6 — NavigationCard behavior: live destination validation gates the
 * Navigate action, failures render inline copy (never a silent no-op),
 * open-only providers replace the field with an explicit Open action, and
 * the provider preference row exposes the Change path.
 */
class NavigationCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun resolved(provider: NavigationProvider) =
        ResolvedNavigationProvider(provider = provider, preferredMissing = false)

    private fun setCard(
        resolved: ResolvedNavigationProvider,
        onHandoff: (NavigationDestination) -> Unit = {},
        onOpenApp: () -> Unit = {},
        onChangeProvider: () -> Unit = {},
        dense: Boolean = false,
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                NavigationCard(
                    resolved = resolved,
                    onHandoff = onHandoff,
                    onOpenApp = onOpenApp,
                    onChangeProvider = onChangeProvider,
                    dense = dense,
                )
            }
        }
    }

    @Test
    fun navigateStaysDisabledUntilDestinationIsValid() {
        setCard(resolved(NavigationProvider.SystemDefault))
        composeRule.onNodeWithText("Navigate").assertIsNotEnabled()
        composeRule.onNode(hasSetTextAction()).performTextInput("Wembley Stadium")
        composeRule.onNodeWithText("Navigate").assertIsEnabled()
    }

    @Test
    fun invalidCoordinatesShowInlineErrorAndBlockNavigate() {
        setCard(resolved(NavigationProvider.SystemDefault))
        composeRule.onNode(hasSetTextAction()).performTextInput("95, 10")
        composeRule.onNodeWithText("Latitude must be -90 to 90 and longitude -180 to 180")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Navigate").assertIsNotEnabled()
    }

    @Test
    fun navigateHandsOffParsedCoordinates() {
        var handed: NavigationDestination? = null
        setCard(resolved(NavigationProvider.GoogleMaps), onHandoff = { handed = it })
        composeRule.onNode(hasSetTextAction()).performTextInput("51.5074, -0.1278")
        composeRule.onNodeWithText("Navigate").performClick()
        assertEquals(NavigationDestination.Coordinates(51.5074, -0.1278), handed)
    }

    @Test
    fun openOnlyProviderHidesDestinationField() {
        setCard(
            resolved(NavigationProvider.OpenOnly("com.example.nav", "Example Nav")),
        )
        composeRule.onNodeWithText("Where to?").assertDoesNotExist()
        composeRule.onNodeWithText("Open Example Nav").assertIsDisplayed()
    }

    @Test
    fun openAppControlOnlyShownForInstalledProviders() {
        setCard(resolved(NavigationProvider.GoogleMaps))
        composeRule.onNodeWithContentDescription("Open Google Maps").assertIsDisplayed()
    }

    @Test
    fun changeInvokesProviderCallback() {
        var changes = 0
        setCard(resolved(NavigationProvider.SystemDefault), onChangeProvider = { changes++ })
        composeRule.onNodeWithText("Change").performClick()
        assertEquals(1, changes)
    }

    @Test
    fun missingPreferredProviderShowsWarning() {
        setCard(
            ResolvedNavigationProvider(
                provider = NavigationProvider.SystemDefault,
                preferredMissing = true,
            ),
        )
        composeRule.onNodeWithText("Preferred app isn\u2019t installed \u2014 using system default")
            .assertIsDisplayed()
    }
}
