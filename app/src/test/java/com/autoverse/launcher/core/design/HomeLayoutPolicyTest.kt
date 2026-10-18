package com.autoverse.launcher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3 — deterministic layout-policy table. Every rule in
 * [homeLayoutPolicyFor] is pinned here, including boundary values, unusual
 * aspect ratios, tiny windows, large font scales, and a sweep asserting the
 * structural invariants that keep rendering clip-free.
 */
class HomeLayoutPolicyTest {

    private fun policy(width: Int, height: Int, fontScale: Float = 1f) =
        homeLayoutPolicyFor(width, height, fontScale)

    // --- Width-class boundaries (tall window so height never gates) ---------

    @Test
    fun `width boundaries decide archetype at tall height`() {
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(599, 900).archetype)
        assertEquals(HomeArchetype.SPLIT_WITH_ACTIONS, policy(600, 900).archetype)
        assertEquals(HomeArchetype.SPLIT_WITH_ACTIONS, policy(839, 900).archetype)
        assertEquals(HomeArchetype.RAIL_SPLIT, policy(840, 900).archetype)
        assertEquals(HomeArchetype.RAIL_SPLIT, policy(1280, 800).archetype)
    }

    // --- Height gates -------------------------------------------------------

    @Test
    fun `split archetypes require minimum height at any width`() {
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(1280, 379).archetype)
        assertEquals(HomeArchetype.RAIL_SPLIT, policy(1280, 380).archetype)
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(600, 379).archetype)
        assertEquals(HomeArchetype.SPLIT_WITH_ACTIONS, policy(600, 380).archetype)
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(1280, 300).archetype)
    }

    @Test
    fun `minimal archetype is used below 240dp tall only`() {
        assertEquals(HomeArchetype.MINIMAL, policy(800, 239).archetype)
        assertEquals(HomeArchetype.MINIMAL, policy(2000, 200).archetype)
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(2000, 240).archetype)
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(800, 240).archetype)
    }

    // --- Navigation density -------------------------------------------------

    @Test
    fun `rail and scroll go full density at 480dp tall`() {
        assertEquals(NavigationDensity.COMPACT, policy(1280, 479).navigationDensity)
        assertEquals(NavigationDensity.FULL, policy(1280, 480).navigationDensity)
        assertEquals(NavigationDensity.FULL, policy(1024, 600).navigationDensity)
        assertEquals(NavigationDensity.FULL, policy(411, 891).navigationDensity)
    }

    @Test
    fun `split needs more height for full density because of its dock row`() {
        assertEquals(NavigationDensity.COMPACT, policy(700, 559).navigationDensity)
        assertEquals(NavigationDensity.FULL, policy(700, 560).navigationDensity)
    }

    @Test
    fun `minimal is always compact density`() {
        assertEquals(NavigationDensity.COMPACT, policy(800, 239).navigationDensity)
        assertEquals(NavigationDensity.COMPACT, policy(1, 1).navigationDensity)
    }

    // --- Status chips progressive disclosure --------------------------------

    @Test
    fun `chips require tall layouts`() {
        assertEquals(StatusChipsMode.NONE, policy(1280, 479).statusChips)
        assertEquals(StatusChipsMode.NONE, policy(700, 559).statusChips)
        assertEquals(StatusChipsMode.NONE, policy(411, 891).statusChips)
        assertEquals(StatusChipsMode.NONE, policy(800, 240).statusChips)
    }

    @Test
    fun `chips disclose by width from reduced to full`() {
        assertEquals(StatusChipsMode.NONE, policy(719, 600).statusChips)
        assertEquals(StatusChipsMode.REDUCED, policy(720, 600).statusChips)
        assertEquals(StatusChipsMode.REDUCED, policy(1024, 600).statusChips)
        assertEquals(StatusChipsMode.REDUCED, policy(1099, 600).statusChips)
        assertEquals(StatusChipsMode.FULL, policy(1100, 600).statusChips)
        assertEquals(StatusChipsMode.FULL, policy(1280, 800).statusChips)
    }

    // --- Font scale ---------------------------------------------------------

    @Test
    fun `large font scale forces the scroll-safe single column`() {
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(1280, 800, 1.16f).archetype)
        assertEquals(NavigationDensity.COMPACT, policy(1280, 800, 1.16f).navigationDensity)
        assertEquals(StatusChipsMode.NONE, policy(1280, 800, 1.16f).statusChips)
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(600, 900, 1.5f).archetype)
    }

    @Test
    fun `font scale at the boundary still allows split`() {
        val atBoundary = policy(1280, 800, HomeLayoutRules.MAX_SPLIT_FONT_SCALE)
        assertEquals(HomeArchetype.RAIL_SPLIT, atBoundary.archetype)
    }

    // --- Unusual aspect ratios and degenerate windows -----------------------

    @Test
    fun `unusual aspect ratios fall back to safe archetypes`() {
        assertEquals(HomeArchetype.MINIMAL, policy(2000, 200).archetype)
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(2000, 280).archetype)
        assertEquals(HomeArchetype.COMPACT_SCROLL, policy(240, 1000).archetype)
        assertEquals(HomeArchetype.SPLIT_WITH_ACTIONS, policy(800, 800).archetype)
        assertEquals(HomeArchetype.RAIL_SPLIT, policy(1000, 1000).archetype)
    }

    @Test
    fun `degenerate windows never throw and stay minimal`() {
        assertEquals(HomeArchetype.MINIMAL, policy(1, 1).archetype)
        assertEquals(HomeArchetype.MINIMAL, policy(0, 0).archetype)
        assertEquals(HomeArchetype.MINIMAL, policy(-50, -50).archetype)
    }

    // --- Invariant sweep ----------------------------------------------------

    @Test
    fun `every combination upholds the structural invariants`() {
        val widths = listOf(1, 320, 411, 599, 600, 720, 839, 840, 1024, 1100, 1280, 1920, 2560)
        val heights = listOf(1, 200, 239, 240, 300, 360, 379, 380, 479, 480, 559, 560, 720, 1080)
        val fontScales = listOf(1f, 1.3f, 2f)

        for (width in widths) {
            for (height in heights) {
                for (fontScale in fontScales) {
                    val context = "w=$width h=$height fontScale=$fontScale"
                    val policy = homeLayoutPolicyFor(width, height, fontScale)

                    if (policy.archetype == HomeArchetype.MINIMAL) {
                        assertTrue(
                            "$context: MINIMAL only below ${HomeLayoutRules.MIN_COMPACT_HEIGHT_DP}dp",
                            height < HomeLayoutRules.MIN_COMPACT_HEIGHT_DP,
                        )
                    }
                    if (policy.archetype == HomeArchetype.RAIL_SPLIT) {
                        assertTrue(
                            "$context: RAIL_SPLIT needs expanded width",
                            width >= HomeLayoutRules.EXPANDED_WIDTH_DP,
                        )
                        assertTrue(
                            "$context: RAIL_SPLIT needs split height",
                            height >= HomeLayoutRules.MIN_SPLIT_HEIGHT_DP,
                        )
                    }
                    if (policy.archetype == HomeArchetype.SPLIT_WITH_ACTIONS) {
                        assertTrue(
                            "$context: SPLIT_WITH_ACTIONS needs medium width",
                            width >= HomeLayoutRules.MEDIUM_WIDTH_DP,
                        )
                        assertTrue(
                            "$context: SPLIT_WITH_ACTIONS needs split height",
                            height >= HomeLayoutRules.MIN_SPLIT_HEIGHT_DP,
                        )
                    }
                    if (fontScale > HomeLayoutRules.MAX_SPLIT_FONT_SCALE) {
                        assertTrue(
                            "$context: large font scale must not use a split archetype",
                            policy.archetype == HomeArchetype.COMPACT_SCROLL ||
                                policy.archetype == HomeArchetype.MINIMAL,
                        )
                    }
                    if (policy.navigationDensity == NavigationDensity.FULL) {
                        assertTrue(
                            "$context: FULL density needs at least " +
                                "${HomeLayoutRules.RAIL_FULL_HEIGHT_DP}dp",
                            height >= HomeLayoutRules.RAIL_FULL_HEIGHT_DP,
                        )
                    }
                    if (policy.statusChips != StatusChipsMode.NONE) {
                        assertTrue(
                            "$context: chips need enough width",
                            width >= HomeLayoutRules.CHIPS_REDUCED_WIDTH_DP,
                        )
                        assertTrue(
                            "$context: chips need a tall layout",
                            height >= HomeLayoutRules.RAIL_FULL_HEIGHT_DP,
                        )
                    }
                }
            }
        }
    }
}
