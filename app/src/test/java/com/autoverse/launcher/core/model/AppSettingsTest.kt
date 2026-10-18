package com.autoverse.launcher.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `defaults are cobalt, system appearance, motion on, balanced profile`() {
        val defaults = AppSettings()
        assertEquals(AutoVerseThemeId.COBALT, defaults.themeId)
        assertEquals(AppearanceMode.SYSTEM, defaults.appearanceMode)
        assertFalse(defaults.reducedMotion)
        assertEquals(PerformanceProfile.BALANCED, defaults.profile)
        assertFalse(defaults.preferSmoothness)
        assertTrue(defaults.clock24h)
        assertEquals(1.0f, defaults.textScale, 0.0001f)
        assertNull(defaults.navigationPackage)
        assertFalse(defaults.embeddedMapEnabled)
    }

    @Test
    fun `appearance mode labels cover the three cabin modes`() {
        assertEquals(listOf("System", "Day", "Night"), AppearanceMode.entries.map { it.displayName })
    }

    @Test
    fun `theme labels cover the five cabin themes`() {
        assertEquals(
            listOf("Cobalt", "Obsidian", "Neon Pulse", "Atelier", "Apex"),
            AutoVerseThemeId.entries.map { it.displayName },
        )
    }
}
