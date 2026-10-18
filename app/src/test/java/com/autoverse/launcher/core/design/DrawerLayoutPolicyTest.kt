package com.autoverse.launcher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 15 — deterministic drawer layout-policy table. Every rule in
 * [appDrawerLayoutPolicyFor] is pinned here, including boundary values,
 * unusual aspect ratios, tiny windows, large font scales, and a sweep
 * asserting the structural invariants that keep the drawer clip-free.
 */
class DrawerLayoutPolicyTest {

    private fun policy(width: Int, height: Int, fontScale: Float = 1f) =
        appDrawerLayoutPolicyFor(width, height, fontScale)

    // --- Width-class boundaries (tall window so height never gates) ---------

    @Test
    fun `width boundaries decide mode at tall height`() {
        assertEquals(AppDrawerMode.COMPACT, policy(599, 900))
        assertEquals(AppDrawerMode.MEDIUM, policy(600, 900))
        assertEquals(AppDrawerMode.MEDIUM, policy(999, 900))
        assertEquals(AppDrawerMode.EXPANDED, policy(1000, 900))
        assertEquals(AppDrawerMode.EXPANDED, policy(1280, 800))
    }

    // --- Height gate --------------------------------------------------------

    @Test
    fun `split modes require minimum height at any width`() {
        assertEquals(AppDrawerMode.COMPACT, policy(1280, 359))
        assertEquals(AppDrawerMode.EXPANDED, policy(1280, 360))
        assertEquals(AppDrawerMode.COMPACT, policy(700, 359))
        assertEquals(AppDrawerMode.MEDIUM, policy(700, 360))
    }

    // --- Font scale ---------------------------------------------------------

    @Test
    fun `large font scale forces the single-column fallback`() {
        assertEquals(AppDrawerMode.COMPACT, policy(1280, 800, 1.16f))
        assertEquals(AppDrawerMode.COMPACT, policy(1000, 800, 1.5f))
        assertEquals(AppDrawerMode.COMPACT, policy(700, 900, 1.3f))
    }

    @Test
    fun `font scale at the boundary still allows split`() {
        assertEquals(
            AppDrawerMode.EXPANDED,
            policy(1280, 800, AppDrawerLayoutRules.MAX_SPLIT_FONT_SCALE),
        )
        assertEquals(
            AppDrawerMode.MEDIUM,
            policy(700, 900, AppDrawerLayoutRules.MAX_SPLIT_FONT_SCALE),
        )
    }

    // --- Unusual aspect ratios and degenerate windows -----------------------

    @Test
    fun `unusual aspect ratios fall back to safe modes`() {
        assertEquals(AppDrawerMode.COMPACT, policy(240, 1000))
        assertEquals(AppDrawerMode.COMPACT, policy(2000, 200))
        assertEquals(AppDrawerMode.MEDIUM, policy(800, 800))
        assertEquals(AppDrawerMode.EXPANDED, policy(1000, 1000))
    }

    @Test
    fun `degenerate windows never throw and stay compact`() {
        assertEquals(AppDrawerMode.COMPACT, policy(1, 1))
        assertEquals(AppDrawerMode.COMPACT, policy(0, 0))
        assertEquals(AppDrawerMode.COMPACT, policy(-50, -50))
    }

    // --- Thresholds ---------------------------------------------------------

    @Test
    fun `threshold constants are pinned`() {
        assertEquals(600, AppDrawerLayoutRules.MEDIUM_WIDTH_DP)
        assertEquals(1000, AppDrawerLayoutRules.EXPANDED_WIDTH_DP)
        assertEquals(360, AppDrawerLayoutRules.MIN_PANEL_HEIGHT_DP)
        assertEquals(1.15f, AppDrawerLayoutRules.MAX_SPLIT_FONT_SCALE, 0.0001f)
    }

    // --- Invariant sweep ----------------------------------------------------

    @Test
    fun `every combination upholds the structural invariants`() {
        val widths = listOf(1, 320, 411, 599, 600, 720, 999, 1000, 1280, 1920, 2560)
        val heights = listOf(1, 200, 359, 360, 480, 720, 800, 1080)
        val fontScales = listOf(1f, 1.15f, 1.16f, 2f)

        for (width in widths) {
            for (height in heights) {
                for (fontScale in fontScales) {
                    val context = "w=$width h=$height fontScale=$fontScale"
                    val mode = appDrawerLayoutPolicyFor(width, height, fontScale)

                    if (mode == AppDrawerMode.EXPANDED) {
                        assertTrue(
                            "$context: EXPANDED needs expanded width",
                            width >= AppDrawerLayoutRules.EXPANDED_WIDTH_DP,
                        )
                        assertTrue(
                            "$context: EXPANDED needs panel height",
                            height >= AppDrawerLayoutRules.MIN_PANEL_HEIGHT_DP,
                        )
                    }
                    if (mode == AppDrawerMode.MEDIUM) {
                        assertTrue(
                            "$context: MEDIUM needs medium width",
                            width >= AppDrawerLayoutRules.MEDIUM_WIDTH_DP,
                        )
                        assertTrue(
                            "$context: MEDIUM needs panel height",
                            height >= AppDrawerLayoutRules.MIN_PANEL_HEIGHT_DP,
                        )
                    }
                    if (fontScale > AppDrawerLayoutRules.MAX_SPLIT_FONT_SCALE) {
                        assertTrue(
                            "$context: large font scale must stay COMPACT",
                            mode == AppDrawerMode.COMPACT,
                        )
                    }
                    if (height < AppDrawerLayoutRules.MIN_PANEL_HEIGHT_DP) {
                        assertTrue(
                            "$context: short windows must stay COMPACT",
                            mode == AppDrawerMode.COMPACT,
                        )
                    }
                }
            }
        }
    }
}
