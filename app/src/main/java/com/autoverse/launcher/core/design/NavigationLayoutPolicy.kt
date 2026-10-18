package com.autoverse.launcher.core.design

/**
 * Phase 18 — Adaptive Layout Engine (navigation page).
 *
 * The places-panel layout is a pure function of the available window space
 * (dp, system insets already accounted for) and the current font scale,
 * mirroring [diagnosticsPanePolicyFor]: no physical-pixel or device-model
 * checks, deterministic rules, table-tested (NavigationLayoutPolicyTest).
 *
 * Decision rules:
 *
 *  1. fontScale > 1.15  -> COMPACT  (large text: panel stacks under the map)
 *  2. height < 360      -> COMPACT  (short strips: panel stacks under the map)
 *  3. width >= 1000     -> WIDE     (panel is a 340dp side column)
 *  4. width >= 600      -> MEDIUM   (panel is a 280dp side column)
 *  5. otherwise         -> COMPACT  (panel stacks under the map, height-capped)
 *
 * WIDE and MEDIUM keep the panel beside the map; COMPACT stacks it below with
 * a bounded height that scrolls internally, so the map always keeps the
 * larger share of the window and the panel can never swallow it.
 */
enum class NavigationPaneMode { COMPACT, MEDIUM, WIDE }

/** Thresholds and measurements in dp. Sizes are available space after insets. */
object NavigationLayoutRules {
    /** Width at which the panel moves beside the map. */
    const val MEDIUM_WIDTH_DP = 600

    /** Width at which the panel gets its widest form. */
    const val WIDE_WIDTH_DP = 1000

    /** Below this height the panel stacks under the map. */
    const val MIN_PANEL_HEIGHT_DP = 360

    /** Above this font scale, side-by-side panes are not safe; stack. */
    const val MAX_SPLIT_FONT_SCALE = 1.15f

    /** Side panel width in the WIDE layout. */
    const val WIDE_SIDE_PANEL_WIDTH_DP = 340

    /** Side panel width in the MEDIUM layout. */
    const val MEDIUM_SIDE_PANEL_WIDTH_DP = 280

    /** Share of the window height the stacked panel may take. */
    const val STACKED_PANEL_HEIGHT_FRACTION = 0.45f

    /** Lower clamp for the stacked panel height. */
    const val MIN_STACKED_PANEL_HEIGHT_DP = 180

    /** Upper clamp for the stacked panel height. */
    const val MAX_STACKED_PANEL_HEIGHT_DP = 320
}

/** Resolved places-panel geometry for one window size. */
data class NavigationPlacesLayout(
    val mode: NavigationPaneMode,
    val sidePanel: Boolean,
    val sidePanelWidthDp: Int,
    val stackedPanelMaxHeightDp: Int,
)

fun navigationPlacesLayoutFor(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
): NavigationPlacesLayout {
    val width = widthDp.coerceAtLeast(1)
    val height = heightDp.coerceAtLeast(1)
    val mode = when {
        fontScale > NavigationLayoutRules.MAX_SPLIT_FONT_SCALE -> NavigationPaneMode.COMPACT
        height < NavigationLayoutRules.MIN_PANEL_HEIGHT_DP -> NavigationPaneMode.COMPACT
        width >= NavigationLayoutRules.WIDE_WIDTH_DP -> NavigationPaneMode.WIDE
        width >= NavigationLayoutRules.MEDIUM_WIDTH_DP -> NavigationPaneMode.MEDIUM
        else -> NavigationPaneMode.COMPACT
    }
    val stackedMaxHeight = (height * NavigationLayoutRules.STACKED_PANEL_HEIGHT_FRACTION)
        .toInt()
        .coerceIn(
            NavigationLayoutRules.MIN_STACKED_PANEL_HEIGHT_DP,
            NavigationLayoutRules.MAX_STACKED_PANEL_HEIGHT_DP,
        )
    return when (mode) {
        NavigationPaneMode.WIDE -> NavigationPlacesLayout(
            mode = mode,
            sidePanel = true,
            sidePanelWidthDp = NavigationLayoutRules.WIDE_SIDE_PANEL_WIDTH_DP,
            stackedPanelMaxHeightDp = stackedMaxHeight,
        )
        NavigationPaneMode.MEDIUM -> NavigationPlacesLayout(
            mode = mode,
            sidePanel = true,
            sidePanelWidthDp = NavigationLayoutRules.MEDIUM_SIDE_PANEL_WIDTH_DP,
            stackedPanelMaxHeightDp = stackedMaxHeight,
        )
        NavigationPaneMode.COMPACT -> NavigationPlacesLayout(
            mode = mode,
            sidePanel = false,
            sidePanelWidthDp = 0,
            stackedPanelMaxHeightDp = stackedMaxHeight,
        )
    }
}
