package com.autoverse.launcher.core.design.icons

import com.autoverse.launcher.core.model.AutoVerseThemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeIconSpecTest {

    @Test
    fun `every registered theme has an icon spec`() {
        for (theme in AutoVerseThemeId.entries) {
            val spec = themeIconSpecFor(theme)
            assertTrue(
                "${theme.name} fell back to the default spec",
                spec !== ThemeIconSpec.Default || theme == AutoVerseThemeId.COBALT,
            )
        }
    }

    @Test
    fun `icon languages map to the prd identities`() {
        assertEquals(IconContainerShape.PRECISION_SQUARE, themeIconSpecFor(AutoVerseThemeId.APEX).containerShape)
        assertEquals(IconContainerShape.PRECISION_SQUARE, themeIconSpecFor(AutoVerseThemeId.OBSIDIAN).containerShape)
        assertEquals(IconContainerShape.SOFT_ROUNDED, themeIconSpecFor(AutoVerseThemeId.ATELIER).containerShape)
        assertEquals(IconContainerShape.SOFT_ROUNDED, themeIconSpecFor(AutoVerseThemeId.COBALT).containerShape)
    }

    @Test
    fun `selected state is structural, never color-only`() {
        for (theme in AutoVerseThemeId.entries) {
            val spec = themeIconSpecFor(theme)
            assertTrue(
                "${theme.name} selectedState must be EDGE, RING or INDICATOR",
                spec.selectedState in IconSelectedState.entries,
            )
        }
    }

    @Test
    fun `spec values stay inside the documented bounds`() {
        for (theme in AutoVerseThemeId.entries) {
            val spec = themeIconSpecFor(theme)
            assertTrue(spec.iconScale in 0.85f..1.15f)
            assertTrue(spec.borderWidthDp in 0.5f..3f)
        }
    }

    @Test
    fun `apex is the sport identity, distinct from cobalt`() {
        assertTrue(
            themeIconSpecFor(AutoVerseThemeId.APEX).containerShape !=
                themeIconSpecFor(AutoVerseThemeId.COBALT).containerShape,
        )
    }
}
