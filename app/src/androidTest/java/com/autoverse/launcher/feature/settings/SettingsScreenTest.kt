package com.autoverse.launcher.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.connectivity.LinkState
import com.autoverse.launcher.core.design.AutoVerseTheme
import com.autoverse.launcher.core.model.AppCategory
import com.autoverse.launcher.core.model.AppSettings
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.NavigationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * Phase 16 — settings behavior pins. The test window is smaller than the
 * WIDE/MEDIUM render sizes, so those layouts are driven through
 * [Modifier.requiredSize] and asserted with existence checks; interactions on
 * nodes the window clips go through the semantics onClick action rather than
 * touch injection. COMPACT tests use the exact window width, so their nodes
 * can be clicked and typed into directly.
 */
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val favoriteApp = InstalledApp(
        packageName = "com.bravo.maps",
        activityName = "com.bravo.maps.MainActivity",
        label = "Bravo Maps",
        category = AppCategory.NAVIGATION,
    )

    private fun setSettings(
        settings: AppSettings = AppSettings(),
        widthDp: Int = 1280,
        heightDp: Int = 800,
        notificationAccessGranted: Boolean = false,
        favoriteApps: List<InstalledApp> = emptyList(),
        navigationProviders: List<NavigationProvider> = emptyList(),
        onSetTheme: (AutoVerseThemeId) -> Unit = {},
        onSetAppearanceMode: (AppearanceMode) -> Unit = {},
        onSetClock24h: (Boolean) -> Unit = {},
        onSetTextScale: (Float) -> Unit = {},
        onSetNavigationPackage: (String?) -> Unit = {},
        onSetProfile: (PerformanceProfile) -> Unit = {},
        onLaunchApp: (InstalledApp) -> Unit = {},
        onOpenNotificationAccessSettings: () -> Boolean = { false },
        onResetSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            AutoVerseTheme(themeId = AutoVerseThemeId.OBSIDIAN) {
                Box(Modifier.requiredSize(widthDp.dp, heightDp.dp)) {
                    SettingsScreen(
                        settings = settings,
                        onSetTheme = onSetTheme,
                        onSetAppearanceMode = onSetAppearanceMode,
                        onSetReducedMotion = {},
                        onSetTextScale = onSetTextScale,
                        onSetProfile = onSetProfile,
                        onSetPreferSmoothness = {},
                        onSetClock24h = onSetClock24h,
                        onBack = {},
                        navigationProviders = navigationProviders,
                        onSetNavigationPackage = onSetNavigationPackage,
                        onSetEmbeddedMapEnabled = {},
                        onOpenMapPreview = {},
                        notificationAccessGranted = notificationAccessGranted,
                        onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                        favoriteApps = favoriteApps,
                        onLaunchApp = onLaunchApp,
                        onRemoveFavorite = {},
                        onOpenAppDrawer = {},
                        loadAppIcon = { null },
                        onOpenDiagnostics = {},
                        onOpenHome = {},
                        onOpenNavigation = {},
                        clockText = "09:41",
                        dateText = "Wednesday, 15 October",
                        connectivity = ConnectivityStatus(
                            wifi = LinkState.CONNECTED,
                            bluetooth = LinkState.CONNECTED,
                        ),
                        onResetSettings = onResetSettings,
                    )
                }
            }
        }
    }

    /**
     * Clicks through the semantics action: works for nodes the undersized test
     * window never actually shows (the WIDE/MEDIUM render extends past it).
     */
    private fun clickNode(text: String) {
        composeRule.onAllNodesWithText(text).onFirst()
            .performSemanticsAction(SemanticsActions.OnClick)
    }

    @Test
    fun wideLayoutShowsCategoriesCenterAndPreview() {
        setSettings()

        composeRule.onAllNodesWithText("Appearance").onFirst().assertExists()
        composeRule.onNodeWithText("THEME").assertExists()
        composeRule.onNodeWithText("DASHBOARD PREVIEW").assertExists()
        composeRule.onNodeWithText("Reset settings to defaults").assertExists()
    }

    @Test
    fun mediumLayoutShowsPreviewAndResetCards() {
        setSettings(widthDp = 800, heightDp = 600)

        composeRule.onNode(hasSetTextAction()).assertExists()
        composeRule.onNodeWithText("DASHBOARD PREVIEW").assertExists()
        composeRule.onNodeWithText("Reset settings to defaults").assertExists()
    }

    @Test
    fun compactLayoutShowsSearchChipsAndPreview() {
        setSettings(widthDp = 411, heightDp = 891)

        composeRule.onNode(hasSetTextAction()).assertExists()
        composeRule.onAllNodesWithText("Appearance").onFirst().assertExists()
        composeRule.onNodeWithText("DASHBOARD PREVIEW").assertExists()
    }

    @Test
    fun themeSelectionFiresCallback() {
        val selected = AtomicReference<AutoVerseThemeId?>(null)
        setSettings(onSetTheme = { selected.set(it) })

        clickNode("Neon Pulse")

        assertEquals(AutoVerseThemeId.NEON_PULSE, selected.get())
    }

    @Test
    fun appearanceModeSelectionFiresCallback() {
        val selected = AtomicReference<AppearanceMode?>(null)
        setSettings(onSetAppearanceMode = { selected.set(it) })

        clickNode("Day")

        assertEquals(AppearanceMode.DAY, selected.get())
    }

    @Test
    fun clockToggleFiresCallback() {
        val toggled = AtomicReference<Boolean?>(null)
        setSettings(onSetClock24h = { toggled.set(it) })

        clickNode("24-hour time")

        assertEquals(false, toggled.get())
    }

    @Test
    fun textSizeResetHiddenAtDefault() {
        setSettings()

        composeRule.onNodeWithText("Reset text size").assertDoesNotExist()
    }

    @Test
    fun textSizeResetFiresCallbackWhenScaled() {
        val scale = AtomicReference<Float?>(null)
        setSettings(
            settings = AppSettings(textScale = 1.25f),
            onSetTextScale = { scale.set(it) },
        )

        clickNode("Reset text size")

        assertEquals(1.0f, scale.get()!!, 0.0001f)
    }

    @Test
    fun searchNoMatchesState() {
        setSettings(widthDp = 411, heightDp = 891)

        composeRule.onNode(hasSetTextAction()).performTextInput("zzzz")

        composeRule.onNodeWithText("NO MATCHES").assertExists()
    }

    @Test
    fun searchResultJumpsToOwningCategory() {
        setSettings(widthDp = 411, heightDp = 891)

        composeRule.onNode(hasSetTextAction()).performTextInput("notification")
        composeRule.onAllNodesWithText("Notification access").onFirst().performClick()

        composeRule.onNodeWithText("NOTIFICATION ACCESS").assertExists()
        composeRule.onNodeWithText("Results for \u201cnotification\u201d").assertDoesNotExist()
    }

    @Test
    fun navigationProviderSelectionFiresCallback() {
        val selected = AtomicReference<String?>(null)
        setSettings(
            navigationProviders = listOf(
                NavigationProvider.GoogleMaps,
                NavigationProvider.Waze,
            ),
            onSetNavigationPackage = { selected.set(it) },
        )

        clickNode("Navigation")
        clickNode("Google Maps")

        assertEquals("com.google.android.apps.maps", selected.get())
    }

    @Test
    fun missingNavigationProviderShowsFallbackNotice() {
        setSettings(
            settings = AppSettings(navigationPackage = "com.missing.app"),
            navigationProviders = listOf(NavigationProvider.GoogleMaps),
        )

        clickNode("Navigation")

        composeRule.onNodeWithText(
            "Preferred app isn\u2019t installed \u2014 using the system default",
        ).assertExists()
    }

    @Test
    fun resetDialogConfirmFiresCallback() {
        val reset = AtomicReference<Boolean?>(null)
        setSettings(onResetSettings = { reset.set(true) })

        clickNode("Reset settings to defaults")
        composeRule.onNodeWithText("Reset settings?").assertExists()

        composeRule.onNodeWithText("Reset").performClick()

        assertEquals(true, reset.get())
    }

    @Test
    fun resetDialogCancelDoesNotFire() {
        val reset = AtomicReference<Boolean?>(null)
        setSettings(onResetSettings = { reset.set(true) })

        clickNode("Reset settings to defaults")
        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.onNodeWithText("Reset settings?").assertDoesNotExist()
        assertEquals(null, reset.get())
    }

    @Test
    fun notificationAccessGrantedIsReflected() {
        setSettings(notificationAccessGranted = true)

        clickNode("Notifications")

        composeRule.onNodeWithText("Granted").assertExists()
        composeRule.onNodeWithText("Open notification access settings").assertExists()
    }

    @Test
    fun notificationAccessNotGrantedIsReflected() {
        setSettings(notificationAccessGranted = false)

        clickNode("Notifications")

        composeRule.onNodeWithText("Not granted").assertExists()
    }

    @Test
    fun performanceProfileSelectionFiresCallback() {
        val profile = AtomicReference<PerformanceProfile?>(null)
        setSettings(onSetProfile = { profile.set(it) })

        clickNode("Performance")

        composeRule.onNodeWithText("Selected").assertExists()
        composeRule.onNodeWithText("Effective").assertExists()

        clickNode("Constrained")

        assertEquals(PerformanceProfile.CONSTRAINED, profile.get())
    }

    @Test
    fun favoriteAppLaunchFiresCallback() {
        val launched = AtomicReference<InstalledApp?>(null)
        setSettings(
            favoriteApps = listOf(favoriteApp),
            onLaunchApp = { launched.set(it) },
        )

        clickNode("Apps & Shortcuts")
        clickNode("Bravo Maps")

        assertEquals(favoriteApp, launched.get())
    }
}
