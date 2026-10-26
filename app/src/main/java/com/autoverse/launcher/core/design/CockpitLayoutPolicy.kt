package com.autoverse.launcher.core.design

import androidx.compose.runtime.Immutable

/**
 * 0.28.2 — Home cockpit geometry (pure, table-tested).
 *
 * Home is always the cockpit: map region ~60% of the usable content area,
 * music ~20%, speedometer (or the incoming-call panel) ~20%. There is no
 * fallback to a card grid any more — earlier builds swapped to a four-card
 * dashboard below 880dp of content width, which is exactly what 1280x720
 * head units at 1.5x density (≈853dp) received.
 *
 *  - SIDE_BY_SIDE (landscape): map takes 60% of the width; music and speed
 *    stack in the remaining 40% column, each half its height (20% + 20%).
 *  - STACKED (portrait / narrow): map takes 60% of the height; music and
 *    speed share the remaining 40% row side by side.
 *
 * The arrangement follows the content area's aspect and a minimum side
 * column width, never a device model or physical pixel size.
 */
enum class CockpitArrangement { SIDE_BY_SIDE, STACKED }

@Immutable
data class CockpitGeometry(
    val arrangement: CockpitArrangement,
    /** Map share of the content width (SIDE_BY_SIDE) or height (STACKED). */
    val mapFraction: Float = CockpitLayoutRules.MAP_FRACTION,
    /** Side panels drop secondary lines (weather, hints) when this is true. */
    val compactPanels: Boolean,
)

object CockpitLayoutRules {
    const val MAP_FRACTION = 0.6f

    /** The 40% side column needs this much width to hold the music controls. */
    const val MIN_SIDE_COLUMN_WIDTH_DP = 220

    /** Below this content height the side panels show only their essentials. */
    const val COMPACT_PANEL_HEIGHT_DP = 420
}

/**
 * [contentWidthDp]/[contentHeightDp] are the area right of the rail and below
 * the top bar, after insets and paddings.
 */
fun cockpitGeometryFor(contentWidthDp: Int, contentHeightDp: Int): CockpitGeometry {
    val width = contentWidthDp.coerceAtLeast(1)
    val height = contentHeightDp.coerceAtLeast(1)
    // sideColumn >= MIN means width * 2/5 >= MIN; comparing in integers keeps
    // the 550dp boundary inclusive (float math makes 550 * 0.4 = 219.99998).
    val sideColumnFits = width * 2 >= CockpitLayoutRules.MIN_SIDE_COLUMN_WIDTH_DP * 5
    val arrangement = if (width >= height && sideColumnFits) {
        CockpitArrangement.SIDE_BY_SIDE
    } else {
        CockpitArrangement.STACKED
    }
    val panelHeight = when (arrangement) {
        CockpitArrangement.SIDE_BY_SIDE -> height / 2f
        CockpitArrangement.STACKED -> height * (1f - CockpitLayoutRules.MAP_FRACTION)
    }
    return CockpitGeometry(
        arrangement = arrangement,
        compactPanels = panelHeight < CockpitLayoutRules.COMPACT_PANEL_HEIGHT_DP / 2f,
    )
}
