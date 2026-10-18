package com.autoverse.launcher.core.design

/**
 * Phase 15 — Adaptive Layout Engine (app drawer).
 *
 * The drawer layout is a pure function of the available window space (dp,
 * system insets already accounted for) and the current font scale, mirroring
 * [homeLayoutPolicyFor]'s philosophy: no physical-pixel or device-model
 * checks, deterministic rules, table-tested (DrawerLayoutPolicyTest).
 *
 * Decision rules (see docs/adaptive-layout.md for the rationale):
 *
 *  1. fontScale > 1.15  -> COMPACT  (large text: single column, no rail)
 *  2. height < 360      -> COMPACT  (short strips: central panel only)
 *  3. width >= 1000     -> EXPANDED (rail + app grid + favorites side panel)
 *  4. width >= 600      -> MEDIUM   (rail + app grid)
 *  5. otherwise         -> COMPACT  (grid with top navigation)
 *
 * COMPACT is the universal fallback: everything collapses into one
 * vertically scrollable column, so a constrained or unusual window degrades
 * to scrolling instead of clipping.
 */
enum class AppDrawerMode { COMPACT, MEDIUM, EXPANDED }

/** Thresholds in dp. Sizes are available space after system insets. */
object AppDrawerLayoutRules {
    /** Width at which the drawer keeps the navigation rail. */
    const val MEDIUM_WIDTH_DP = 600

    /** Width at which the favorites side panel becomes available. */
    const val EXPANDED_WIDTH_DP = 1000

    /** Below this height the drawer keeps the single central panel. */
    const val MIN_PANEL_HEIGHT_DP = 360

    /** Above this font scale, side-by-side panels are not safe; stack. */
    const val MAX_SPLIT_FONT_SCALE = 1.15f
}

fun appDrawerLayoutPolicyFor(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
): AppDrawerMode {
    val width = widthDp.coerceAtLeast(1)
    val height = heightDp.coerceAtLeast(1)
    return when {
        fontScale > AppDrawerLayoutRules.MAX_SPLIT_FONT_SCALE -> AppDrawerMode.COMPACT
        height < AppDrawerLayoutRules.MIN_PANEL_HEIGHT_DP -> AppDrawerMode.COMPACT
        width >= AppDrawerLayoutRules.EXPANDED_WIDTH_DP -> AppDrawerMode.EXPANDED
        width >= AppDrawerLayoutRules.MEDIUM_WIDTH_DP -> AppDrawerMode.MEDIUM
        else -> AppDrawerMode.COMPACT
    }
}
