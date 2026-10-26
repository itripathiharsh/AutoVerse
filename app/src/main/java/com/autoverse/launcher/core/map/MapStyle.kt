package com.autoverse.launcher.core.map

/**
 * Live-map configuration (pure, JVM-tested). The map is MapLibre Native
 * (GPU-rendered vector tiles, BSD licence) fed by OpenFreeMap: free, no API
 * key, no request limits, commercial use allowed — the only obligation is the
 * visible OpenStreetMap attribution in [ATTRIBUTION]. Every URL lives here so
 * the tile host can be swapped (self-hosted OpenFreeMap, bundled PMTiles)
 * without touching UI code.
 */
object MapStyle {
    const val STYLE_HOST = "https://tiles.openfreemap.org/styles/"
    const val NIGHT_STYLE = "dark"
    const val DAY_STYLE = "positron"

    /** OpenStreetMap's ODbL requires this to stay visible on the map. */
    const val ATTRIBUTION = "© OpenFreeMap © OpenMapTiles © OpenStreetMap contributors"

    /** Driving zoom while following the car; city streets stay readable. */
    const val FOLLOW_ZOOM = 16.0

    /** Overview zoom shown before the first fix (country scale, no fake position). */
    const val NO_FIX_ZOOM = 3.0

    /**
     * Ambient tile cache. Tiles already seen render instantly and survive
     * dead zones; 200 MB holds a city plus regular routes without bloating
     * storage on 16 GB head units.
     */
    const val AMBIENT_CACHE_BYTES = 200L * 1024 * 1024

    /** Puck/camera animation never runs faster than the map itself. */
    const val LOCATION_ANIMATION_MAX_FPS = 30

    fun styleUrl(night: Boolean): String = STYLE_HOST + if (night) NIGHT_STYLE else DAY_STYLE

    /**
     * Below walking speed GPS bearing is noise, so the camera stops rotating
     * with it (north-up) and only follows position.
     */
    fun rotateWithHeading(speedKmh: Int?): Boolean = (speedKmh ?: 0) >= HEADING_MIN_SPEED_KMH

    private const val HEADING_MIN_SPEED_KMH = 8
}
