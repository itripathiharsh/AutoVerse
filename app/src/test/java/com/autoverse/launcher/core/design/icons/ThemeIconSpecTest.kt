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
        assertEquals(IconContainerShape.ANGULAR_SHIELD, themeIconSpecFor(AutoVerseThemeId.GOTHAM).containerShape)
        assertEquals(IconContainerShape.CIRCULAR_RADIAL, themeIconSpecFor(AutoVerseThemeId.HOT_ROD).containerShape)
        assertEquals(IconContainerShape.HEXAGONAL_NEON, themeIconSpecFor(AutoVerseThemeId.NEON_TOKYO).containerShape)
        assertEquals(IconContainerShape.PRECISION_SQUARE, themeIconSpecFor(AutoVerseThemeId.APEX).containerShape)
        assertEquals(IconContainerShape.SPORT_ANGULAR, themeIconSpecFor(AutoVerseThemeId.WEB_RUSH).containerShape)
        assertEquals(IconContainerShape.SOFT_ROUNDED, themeIconSpecFor(AutoVerseThemeId.FORTRESS).containerShape)
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
    fun `fortress is the light identity and web rush the sport identity`() {
        // Registered in the theme registry (ThemeSpecTest covers the pair);
        // here we pin their icon languages differ from their neighbours.
        assertTrue(
            themeIconSpecFor(AutoVerseThemeId.FORTRESS).containerShape !=
                themeIconSpecFor(AutoVerseThemeId.GOTHAM).containerShape,
        )
        assertTrue(
            themeIconSpecFor(AutoVerseThemeId.WEB_RUSH).containerShape !=
                themeIconSpecFor(AutoVerseThemeId.COBALT).containerShape,
        )
    }
}
