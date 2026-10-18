package com.autoverse.launcher.core.design

import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.AutoVerseThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThemeSpecTest {

    @Test
    fun `every theme id is registered exactly once`() {
        assertEquals(AutoVerseThemeId.entries.size, AutoVerseThemeSpecs.size)
        AutoVerseThemeId.entries.forEach { id ->
            assertEquals(1, AutoVerseThemeSpecs.count { it.id == id })
        }
    }

    @Test
    fun `every registered spec matches its theme id and display name`() {
        AutoVerseThemeId.entries.forEach { id ->
            val spec = themeSpecFor(id)
            assertEquals(id, spec.id)
            assertEquals(id.displayName, spec.displayName)
        }
    }

    @Test
    fun `every theme has distinct dark and light palettes`() {
        AutoVerseThemeId.entries.forEach { id ->
            val spec = themeSpecFor(id)
            assertNotEquals(spec.darkColors, spec.lightColors)
        }
    }

    @Test
    fun `no two themes share a palette`() {
        val palettes = AutoVerseThemeSpecs.flatMap { listOf(it.darkColors, it.lightColors) }
        assertEquals(palettes.size, palettes.distinct().size)
    }

    @Test
    fun `colorsFor picks the palette by dark flag`() {
        AutoVerseThemeId.entries.forEach { id ->
            val spec = themeSpecFor(id)
            assertEquals(spec.darkColors, spec.colorsFor(darkTheme = true))
            assertEquals(spec.lightColors, spec.colorsFor(darkTheme = false))
        }
    }

    @Test
    fun `system appearance follows the system flag`() {
        assertEquals(true, isDarkTheme(AppearanceMode.SYSTEM, systemInDark = true))
        assertEquals(false, isDarkTheme(AppearanceMode.SYSTEM, systemInDark = false))
    }

    @Test
    fun `day mode forces light even when the system is dark`() {
        assertEquals(false, isDarkTheme(AppearanceMode.DAY, systemInDark = true))
    }

    @Test
    fun `night mode forces dark even when the system is light`() {
        assertEquals(true, isDarkTheme(AppearanceMode.NIGHT, systemInDark = false))
    }
}
