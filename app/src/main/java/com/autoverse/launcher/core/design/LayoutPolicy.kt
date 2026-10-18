package com.autoverse.launcher.core.design

import androidx.compose.runtime.Immutable

/**
 * Phase 3 — Adaptive Layout Engine.
 *
 * The home dashboard layout is a pure function of the available window space
 * (in dp, insets already accounted for) and the current font scale. No
 * physical-pixel or device-model checks are allowed here: the same rules must
 * hold on any display, in any window size, and must be verifiable by plain
 * unit tests.
 *
 * Decision rules (see docs/adaptive-layout.md for the full rationale and the
 * measured heights each constant is derived from):
 *
 *  1. height < 240dp            -> MINIMAL   (very constrained strips)
 *  2. fontScale > 1.15          -> COMPACT_SCROLL (single column, scroll-safe)
 *  3. width >= 840 && h >= 380  -> RAIL_SPLIT
 *  4. width >= 600 && h >= 380  -> SPLIT_WITH_ACTIONS
 *  5. otherwise                 -> COMPACT_SCROLL (scroll-safe)
 *
 *  Navigation density: FULL from 480dp tall (RAIL/scroll), 560dp (split with
 *  its dedicated dock row); COMPACT below that or on large font scales.
 *  Status chips disclose progressively: NONE -> REDUCED (theme + FPS) at
 *  >= 720dp wide -> FULL set at >= 1100dp wide, only on tall-enough layouts.
 *
 * Every rule is deterministic, table-tested (HomeLayoutPolicyTest), and every
 * fallback archetype (COMPACT_SCROLL, MINIMAL) is vertically scrollable, so a
 * constrained or unusual window degrades to scrolling instead of clipping.
 */
enum class HomeArchetype { MINIMAL, COMPACT_SCROLL, SPLIT_WITH_ACTIONS, RAIL_SPLIT }

enum class NavigationDensity { FULL, COMPACT }

enum class StatusChipsMode { NONE, REDUCED, FULL }

@Immutable
data class HomeLayoutPolicy(
    val archetype: HomeArchetype,
    val statusChips: StatusChipsMode,
    val navigationDensity: NavigationDensity,
)

/** Thresholds in dp. Sizes are available space after system insets. */
object HomeLayoutRules {
    /** Width at which the dashboard may leave the single-column form. */
    const val MEDIUM_WIDTH_DP = 600

    /** Width at which the navigation rail form becomes available. */
    const val EXPANDED_WIDTH_DP = 840

    /** Below this height only the minimal strip layout is used. */
    const val MIN_COMPACT_HEIGHT_DP = 240

    /** Minimum height for any two-pane (split) archetype. */
    const val MIN_SPLIT_HEIGHT_DP = 380

    /** Height for full-sized navigation/media compositions in RAIL/scroll. */
    const val RAIL_FULL_HEIGHT_DP = 480

    /** Height for full-sized compositions in SPLIT (dock row costs extra). */
    const val SPLIT_FULL_HEIGHT_DP = 560

    /** Chips need this much width before even the reduced set appears. */
    const val CHIPS_REDUCED_WIDTH_DP = 720

    /** The full chip set needs this much width. */
    const val CHIPS_FULL_WIDTH_DP = 1100

    /** Above this font scale, split archetypes are not safe; scroll instead. */
    const val MAX_SPLIT_FONT_SCALE = 1.15f
}

fun homeLayoutPolicyFor(
    widthDp: Int,
    heightDp: Int,
    fontScale: Float = 1f,
): HomeLayoutPolicy {
    val width = widthDp.coerceAtLeast(1)
    val height = heightDp.coerceAtLeast(1)

    val archetype = when {
        height < HomeLayoutRules.MIN_COMPACT_HEIGHT_DP -> HomeArchetype.MINIMAL
        fontScale > HomeLayoutRules.MAX_SPLIT_FONT_SCALE -> HomeArchetype.COMPACT_SCROLL
        width >= HomeLayoutRules.EXPANDED_WIDTH_DP &&
            height >= HomeLayoutRules.MIN_SPLIT_HEIGHT_DP -> HomeArchetype.RAIL_SPLIT
        width >= HomeLayoutRules.MEDIUM_WIDTH_DP &&
            height >= HomeLayoutRules.MIN_SPLIT_HEIGHT_DP -> HomeArchetype.SPLIT_WITH_ACTIONS
        else -> HomeArchetype.COMPACT_SCROLL
    }

    val navigationDensity = when {
        fontScale > HomeLayoutRules.MAX_SPLIT_FONT_SCALE -> NavigationDensity.COMPACT
        archetype == HomeArchetype.MINIMAL -> NavigationDensity.COMPACT
        archetype == HomeArchetype.SPLIT_WITH_ACTIONS ->
            if (height >= HomeLayoutRules.SPLIT_FULL_HEIGHT_DP) {
                NavigationDensity.FULL
            } else {
                NavigationDensity.COMPACT
            }
        else ->
            if (height >= HomeLayoutRules.RAIL_FULL_HEIGHT_DP) {
                NavigationDensity.FULL
            } else {
                NavigationDensity.COMPACT
            }
    }

    val chipsHeightOk = when (archetype) {
        HomeArchetype.RAIL_SPLIT -> height >= HomeLayoutRules.RAIL_FULL_HEIGHT_DP
        HomeArchetype.SPLIT_WITH_ACTIONS -> height >= HomeLayoutRules.SPLIT_FULL_HEIGHT_DP
        HomeArchetype.COMPACT_SCROLL, HomeArchetype.MINIMAL -> false
    }
    val statusChips = when {
        !chipsHeightOk || width < HomeLayoutRules.CHIPS_REDUCED_WIDTH_DP -> StatusChipsMode.NONE
        width >= HomeLayoutRules.CHIPS_FULL_WIDTH_DP -> StatusChipsMode.FULL
        else -> StatusChipsMode.REDUCED
    }

    return HomeLayoutPolicy(
        archetype = archetype,
        statusChips = statusChips,
        navigationDensity = navigationDensity,
    )
}
