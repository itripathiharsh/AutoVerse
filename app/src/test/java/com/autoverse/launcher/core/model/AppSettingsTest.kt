package com.autoverse.launcher.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `defaults are cobalt, system appearance, reduced motion, constrained profile`() {
        val defaults = AppSettings()
        assertEquals(AutoVerseThemeId.COBALT, defaults.themeId)
        assertEquals(AppearanceMode.NIGHT, defaults.appearanceMode)
        assertFalse(defaults.reducedMotion)
        assertEquals(PerformanceProfile.CONSTRAINED, defaults.profile)
        assertTrue(defaults.preferSmoothness)
        assertTrue(defaults.clock24h)
        assertEquals(1.0f, defaults.textScale, 0.0001f)
        // Google Maps is the default handoff target; side panels start expanded.
        assertEquals("com.google.android.apps.maps", defaults.navigationPackage)
        assertFalse(defaults.appsRecentCollapsed)
        assertFalse(defaults.mapsPlacesCollapsed)
    }

    @Test
    fun `appearance mode labels cover the three cabin modes`() {
        assertEquals(listOf("System", "Day", "Night"), AppearanceMode.entries.map { it.displayName })
    }

    @Test
    fun `theme labels cover the four cabin themes in picker order`() {
        assertEquals(
            listOf("Cobalt", "Obsidian", "Atelier", "Apex"),
            AutoVerseThemeId.entries.map { it.displayName },
        )
    }

    @Test
    fun `every theme is included - none locked behind premium`() {
        assertTrue(AutoVerseThemeId.entries.none { it.premium })
    }

    @Test
    fun `paid 1_0 defaults - every theme included, onboarding pending`() {
        val defaults = AppSettings()
        assertTrue(defaults.premiumUnlocked)
        assertFalse(defaults.onboardingComplete)
    }
}
