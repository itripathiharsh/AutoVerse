package com.autoverse.launcher.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 15 — drawer chip contract. [AppCategory.label] is the single source
 * of truth for filter-chip wording and the drawer chip row iterates the enum;
 * both the labels and the declaration order are pinned here so a rename or
 * reorder is a deliberate, visible change instead of a silent UI regression.
 */
class AppCategoryTest {

    @Test
    fun `chip labels are pinned`() {
        assertEquals("All Apps", AppCategory.ALL.label)
        assertEquals("Navigation", AppCategory.NAVIGATION.label)
        assertEquals("Music & Audio", AppCategory.MUSIC_AUDIO.label)
        assertEquals("Communication", AppCategory.COMMUNICATION.label)
        assertEquals("Video", AppCategory.VIDEO.label)
        assertEquals("Tools", AppCategory.TOOLS.label)
        assertEquals("Others", AppCategory.OTHERS.label)
    }

    @Test
    fun `declaration order is the chip row order`() {
        assertEquals(
            listOf(
                AppCategory.ALL,
                AppCategory.NAVIGATION,
                AppCategory.MUSIC_AUDIO,
                AppCategory.COMMUNICATION,
                AppCategory.VIDEO,
                AppCategory.TOOLS,
                AppCategory.OTHERS,
            ),
            AppCategory.entries.toList(),
        )
    }

    @Test
    fun `new apps default to a concrete category, never the ALL sentinel`() {
        val app = InstalledApp("com.example.app", "com.example.app.MainActivity", "Example")
        assertEquals(AppCategory.OTHERS, app.category)
        assertTrue(app.category != AppCategory.ALL)
    }

    @Test
    fun `labels are unique`() {
        val labels = AppCategory.entries.map { it.label }
        assertEquals(labels.size, labels.toSet().size)
    }
}
