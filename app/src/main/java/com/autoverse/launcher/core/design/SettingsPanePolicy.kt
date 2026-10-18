package com.autoverse.launcher.core.design

/**
 * Phase 16 — Adaptive Layout Engine (settings).
 *
 * The settings layout is a pure function of the available window space (dp,
 * system insets already accounted for) and the current font scale, mirroring
 * [appDrawerLayoutPolicyFor]'s philosophy: no physical-pixel or device-model
 * checks, deterministic rules, table-tested (SettingsPanePolicyTest).
 *
 * Decision rules (see docs/adaptive-layout.md for the rationale):
 *
 *  1. fontScale > 1.15  -> COMPACT  (large text: one column, no side panes)
 *  2. height < 360      -> COMPACT  (short strips: central column only)
 *  3. width >= 1000     -> WIDE     (categories | controls | live preview)
 *  4. width >= 600      -> MEDIUM   (categories beside controls, preview below)
 *  5. otherwise         -> COMPACT  (chips + controls in one column)
 *
 * COMPACT is the universal fallback: every pane collapses into one vertically
 * scrollable column, so a constrained or unusual window degrades to scrolling
 * instead of clipping.
 */
enum class SettingsPaneMode { COMPACT, MEDIUM, WIDE }

/** Thresholds in dp. Sizes are available space after system insets. */
object SettingsPaneLayoutRules {
    /** Width at which the categories pane sits next to the controls pane. */
    const val MEDIUM_WIDTH_DP = 600

    /** Width at which the live preview gains its own right-hand panel. */
    const val WIDE_WIDTH_DP = 1000

    /** Below this height the screen keeps a single scrollable column. */
    const val MIN_PANEL_HEIGHT_DP = 360

    /** Above this font scale, side-by-side panes are not safe; stack. */
    const val MAX_SPLIT_FONT_SCALE = 1.15f
}

fun settingsPanePolicyFor(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
): SettingsPaneMode {
    val width = widthDp.coerceAtLeast(1)
    val height = heightDp.coerceAtLeast(1)
    return when {
        fontScale > SettingsPaneLayoutRules.MAX_SPLIT_FONT_SCALE -> SettingsPaneMode.COMPACT
        height < SettingsPaneLayoutRules.MIN_PANEL_HEIGHT_DP -> SettingsPaneMode.COMPACT
        width >= SettingsPaneLayoutRules.WIDE_WIDTH_DP -> SettingsPaneMode.WIDE
        width >= SettingsPaneLayoutRules.MEDIUM_WIDTH_DP -> SettingsPaneMode.MEDIUM
        else -> SettingsPaneMode.COMPACT
    }
}
