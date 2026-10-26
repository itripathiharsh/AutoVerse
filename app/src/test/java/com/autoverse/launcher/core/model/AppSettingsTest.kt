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
        assertEquals(AppearanceMode.SYSTEM, defaults.appearanceMode)
        assertTrue(defaults.reducedMotion)
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
    fun `theme labels cover the twelve cabin themes in picker order`() {
        assertEquals(
            listOf("Cobalt", "Obsidian", "Neon Pulse", "Atelier", "Apex",
                "Sakura", "Neon Tokyo", "Shonen", "Gotham", "Hot Rod",
                "Fortress", "Web Rush"),
            AutoVerseThemeId.entries.map { it.displayName },
        )
    }

    @Test
    fun `premium flag marks exactly the seven premium themes`() {
        assertEquals(
            listOf("Sakura", "Neon Tokyo", "Shonen", "Gotham", "Hot Rod",
                "Fortress", "Web Rush"),
            AutoVerseThemeId.entries.filter { it.premium }.map { it.displayName },
        )
    }

    @Test
    fun `tier 1 defaults - premium locked, onboarding pending`() {
        val defaults = AppSettings()
        assertFalse(defaults.premiumUnlocked)
        assertFalse(defaults.onboardingComplete)
    }
}
