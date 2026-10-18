package com.autoverse.launcher.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 16 — pins the settings catalog shape and the search behavior. The
 * catalog must cover every category (the navigation renders one entry per
 * enum value) and every entry must be reachable by its own title.
 */
class SettingsCatalogTest {

    @Test
    fun `catalog covers every category`() {
        val covered = SETTINGS_CATALOG.map { it.category }.toSet()
        assertEquals(SettingsCategory.entries.toSet(), covered)
    }

    @Test
    fun `every category has at least one searchable entry`() {
        for (category in SettingsCategory.entries) {
            assertTrue(
                "no catalog entry for ${category.label}",
                SETTINGS_CATALOG.any { it.category == category },
            )
        }
    }

    @Test
    fun `titles are unique inside each category`() {
        for (category in SettingsCategory.entries) {
            val titles = SETTINGS_CATALOG.filter { it.category == category }.map { it.title }
            assertEquals(
                "duplicate titles in ${category.label}",
                titles.size,
                titles.toSet().size,
            )
        }
    }

    @Test
    fun `every entry is found by its own title`() {
        for (entry in SETTINGS_CATALOG) {
            val results = searchSettings(entry.title)
            assertTrue(
                "'${entry.title}' did not find its own entry",
                entry in results,
            )
        }
    }

    @Test
    fun `search is case insensitive and trims whitespace`() {
        assertEquals(
            searchSettings("theme"),
            searchSettings("  THEME  "),
        )
        assertTrue(searchSettings("Bluetooth-free text").isEmpty()) // no accidental match
    }

    @Test
    fun `search matches subtitles and category labels`() {
        val bySubtitle = searchSettings("Google Maps")
        assertTrue(bySubtitle.any { it.title == "Navigation app" })

        val byCategory = searchSettings("Appearance")
        assertEquals(
            SETTINGS_CATALOG.count { it.category == SettingsCategory.APPEARANCE },
            byCategory.size,
        )
    }

    @Test
    fun `blank queries return no results`() {
        assertTrue(searchSettings("").isEmpty())
        assertTrue(searchSettings("   ").isEmpty())
    }

    @Test
    fun `search hits are data, not UI`() {
        val results = searchSettings("reset")
        assertTrue(results.any { it.category == SettingsCategory.ABOUT })
        assertFalse(results.any { it.category == SettingsCategory.NAVIGATION })
    }
}
