package com.autoverse.launcher.core.design

/**
 * 0.28.2 — Maps page geometry (pure, table-tested).
 *
 * The Google Maps region takes ~70% of the content width beside the rail and
 * destinations ~30%. When the 30% column would be too narrow for a readable
 * destination row, the page stacks the two panels instead.
 */
object MapsLayoutRules {
    const val MAP_FRACTION = 0.7f

    /** Narrowest destinations column that still fits a row plus its remove button. */
    const val MIN_PLACES_COLUMN_DP = 200
}

/** True when the map region and destinations fit side by side in [contentWidthDp]. */
fun mapsPanelsSideBySide(contentWidthDp: Int): Boolean =
    contentWidthDp * (1f - MapsLayoutRules.MAP_FRACTION) >= MapsLayoutRules.MIN_PLACES_COLUMN_DP
