package com.autoverse.launcher.core.design

/**
 * 0.28.2 — Apps page geometry (pure, table-tested).
 *
 * All Apps takes ~70% and Recently Used ~30% of the content width beside the
 * rail. When the 30% column would be too narrow for a readable app row, the
 * page stacks instead: Recently Used becomes a one-row strip above the grid.
 * There is never a map on this page.
 */
object AppsLayoutRules {
    const val ALL_APPS_FRACTION = 0.7f

    /** Narrowest Recently Used column that still fits icon + label rows. */
    const val MIN_RECENT_COLUMN_DP = 200

    /** Grid cell minimum; ~5 columns on a 720p head unit at 1.5x density. */
    const val TILE_MIN_WIDTH_DP = 112
}

/** True when All Apps and Recently Used fit side by side in [contentWidthDp]. */
fun appsPanelsSideBySide(contentWidthDp: Int): Boolean =
    contentWidthDp * (1f - AppsLayoutRules.ALL_APPS_FRACTION) >= AppsLayoutRules.MIN_RECENT_COLUMN_DP
