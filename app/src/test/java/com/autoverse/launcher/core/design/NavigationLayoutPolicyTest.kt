package com.autoverse.launcher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 18 — the navigation places-panel layout is a pure function of window
 * size and font scale. This table pins every boundary: large text or short
 * strips stack the panel, wide windows move it beside the map at a fixed
 * width, and the stacked height always stays clamped.
 */
class NavigationLayoutPolicyTest {

    @Test
    fun `font scale above the split maximum forces the stacked layout`() {
        val layout = navigationPlacesLayoutFor(1400, 900, fontScale = 1.16f)
        assertEquals(NavigationPaneMode.COMPACT, layout.mode)
        assertFalse(layout.sidePanel)
    }

    @Test
    fun `font scale exactly at the split maximum still splits`() {
        val layout = navigationPlacesLayoutFor(1200, 800, fontScale = 1.15f)
        assertEquals(NavigationPaneMode.WIDE, layout.mode)
    }

    @Test
    fun `a short window stacks the panel even when wide`() {
        val layout = navigationPlacesLayoutFor(1200, 359)
        assertEquals(NavigationPaneMode.COMPACT, layout.mode)
    }

    @Test
    fun `height exactly at the minimum still splits when wide`() {
        val layout = navigationPlacesLayoutFor(1200, 360)
        assertEquals(NavigationPaneMode.WIDE, layout.mode)
    }

    @Test
    fun `width at the wide threshold uses the wide side panel`() {
        val layout = navigationPlacesLayoutFor(1000, 600)
        assertEquals(NavigationPaneMode.WIDE, layout.mode)
        assertTrue(layout.sidePanel)
        assertEquals(340, layout.sidePanelWidthDp)
    }

    @Test
    fun `width just below the wide threshold uses the medium side panel`() {
        val layout = navigationPlacesLayoutFor(999, 600)
        assertEquals(NavigationPaneMode.MEDIUM, layout.mode)
        assertTrue(layout.sidePanel)
        assertEquals(280, layout.sidePanelWidthDp)
    }

    @Test
    fun `width at the medium threshold uses the medium side panel`() {
        val layout = navigationPlacesLayoutFor(600, 800)
        assertEquals(NavigationPaneMode.MEDIUM, layout.mode)
        assertEquals(280, layout.sidePanelWidthDp)
    }

    @Test
    fun `width just below the medium threshold stacks the panel`() {
        val layout = navigationPlacesLayoutFor(599, 800)
        assertEquals(NavigationPaneMode.COMPACT, layout.mode)
        assertFalse(layout.sidePanel)
        assertEquals(0, layout.sidePanelWidthDp)
    }

    @Test
    fun `degenerate sizes coerce and stack rather than crash`() {
        val layout = navigationPlacesLayoutFor(0, 0)
        assertEquals(NavigationPaneMode.COMPACT, layout.mode)
    }

    @Test
    fun `stacked panel height uses the fraction inside the clamps`() {
        val layout = navigationPlacesLayoutFor(599, 500)
        assertEquals(225, layout.stackedPanelMaxHeightDp)
    }

    @Test
    fun `stacked panel height clamps to the upper bound`() {
        val layout = navigationPlacesLayoutFor(599, 800)
        assertEquals(320, layout.stackedPanelMaxHeightDp)
    }

    @Test
    fun `stacked panel height clamps to the lower bound`() {
        val layout = navigationPlacesLayoutFor(1200, 300)
        assertEquals(180, layout.stackedPanelMaxHeightDp)
    }

    @Test
    fun `side panel layouts still carry the stacked clamp`() {
        val layout = navigationPlacesLayoutFor(1000, 800)
        assertEquals(320, layout.stackedPanelMaxHeightDp)
    }

    @Test
    fun `threshold constants are the documented values`() {
        assertEquals(600, NavigationLayoutRules.MEDIUM_WIDTH_DP)
        assertEquals(1000, NavigationLayoutRules.WIDE_WIDTH_DP)
        assertEquals(360, NavigationLayoutRules.MIN_PANEL_HEIGHT_DP)
        assertEquals(1.15f, NavigationLayoutRules.MAX_SPLIT_FONT_SCALE, 0.0f)
    }
}
