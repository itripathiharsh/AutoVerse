package com.autoverse.launcher.core.design

/**
 * Phase 17 — Adaptive Layout Engine (diagnostics).
 *
 * The diagnostics layout is a pure function of the available window space
 * (dp, system insets already accounted for) and the current font scale,
 * mirroring [settingsPanePolicyFor]: no physical-pixel or device-model
 * checks, deterministic rules, table-tested (DiagnosticsLayoutPolicyTest).
 *
 * Decision rules:
 *
 *  1. fontScale > 1.15  -> COMPACT  (large text: one column, no side panes)
 *  2. height < 360      -> COMPACT  (short strips: central column only)
 *  3. width >= 1000     -> WIDE     (rail + two information columns)
 *  4. width >= 600      -> MEDIUM   (top bar above a single scroll column)
 *  5. otherwise         -> COMPACT  (one scroll column, densest stacking)
 *
 * COMPACT is the universal fallback: every panel collapses into one
 * vertically scrollable column, so a constrained or unusual window degrades
 * to scrolling instead of clipping.
 */
enum class DiagnosticsPaneMode { COMPACT, MEDIUM, WIDE }

/** Thresholds in dp. Sizes are available space after system insets. */
object DiagnosticsLayoutRules {
    /** Width at which the top bar sits above a comfortable reading column. */
    const val MEDIUM_WIDTH_DP = 600

    /** Width at which the rail and a second information column appear. */
    const val WIDE_WIDTH_DP = 1000

    /** Below this height the screen keeps a single scrollable column. */
    const val MIN_PANEL_HEIGHT_DP = 360

    /** Above this font scale, side-by-side panes are not safe; stack. */
    const val MAX_SPLIT_FONT_SCALE = 1.15f
}

fun diagnosticsPanePolicyFor(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
): DiagnosticsPaneMode {
    val width = widthDp.coerceAtLeast(1)
    val height = heightDp.coerceAtLeast(1)
    return when {
        fontScale > DiagnosticsLayoutRules.MAX_SPLIT_FONT_SCALE -> DiagnosticsPaneMode.COMPACT
        height < DiagnosticsLayoutRules.MIN_PANEL_HEIGHT_DP -> DiagnosticsPaneMode.COMPACT
        width >= DiagnosticsLayoutRules.WIDE_WIDTH_DP -> DiagnosticsPaneMode.WIDE
        width >= DiagnosticsLayoutRules.MEDIUM_WIDTH_DP -> DiagnosticsPaneMode.MEDIUM
        else -> DiagnosticsPaneMode.COMPACT
    }
}
