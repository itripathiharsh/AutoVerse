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
    /**
     * OpenFreeMap "liberty", pinned in assets and recoloured for night by
     * [MapStyleJson]. Tiles, glyphs and sprites still come from the keyless
     * OpenFreeMap host the style references.
     */
    const val STYLE_ASSET = "map/liberty.json"
    const val TILE_HOST = "https://tiles.openfreemap.org/"

    /** Stable keys for the two built styles (day/night switch reloads by key). */
    fun styleKey(night: Boolean): String = if (night) "night" else "day"

    /** OpenStreetMap's ODbL requires this to stay visible on the map. */
    const val ATTRIBUTION = "© OpenFreeMap © OpenMapTiles © OpenStreetMap contributors"

    /** Driving zoom while following the car; city streets stay readable. */
    const val FOLLOW_ZOOM = 16.0

    /** Overview zoom shown before the first fix (country scale, no fake position). */
    const val NO_FIX_ZOOM = 4.0

    /**
     * Where the camera starts when the unit has never had a fix: the middle of
     * India (the launch market), at country zoom — never 0,0 in the ocean.
     */
    const val DEFAULT_CENTER_LAT = 22.5
    const val DEFAULT_CENTER_LNG = 79.0

    /** Closer, heading-up zoom while turn-by-turn guidance runs. */
    const val GUIDANCE_ZOOM = 16.5

    /** Driving perspective: tilted so the road ahead fills the view. */
    const val GUIDANCE_TILT = 50.0

    /** Lighter tilt on entry-level units (a pitched view loads more tiles). */
    const val GUIDANCE_TILT_LITE = 35.0

    /**
     * Share of the map height kept above the car while guiding, so the car
     * sits low in the view and most of the screen shows the road ahead.
     */
    const val GUIDANCE_LOOK_AHEAD = 0.42

    /**
     * Guidance zoom by speed: close in town, further out on highways so the
     * next turn appears early. Pure for JVM tests.
     */
    fun guidanceZoomFor(speedKmh: Int?): Double = when {
        speedKmh == null || speedKmh < 35 -> 17.0
        speedKmh < 70 -> 16.4
        speedKmh < 95 -> 15.8
        else -> 15.2
    }

    /** Zoom used when reopening at the last remembered car position. */
    const val REMEMBERED_ZOOM = 14.0

    /** The remembered position is written at most this often (cheap prefs write). */
    const val REMEMBER_POSITION_EVERY_MS = 30_000L

    /**
     * Start camera: the last remembered car position when there is one,
     * otherwise the India overview. Pure for JVM tests.
     */
    fun startCamera(rememberedLat: Double?, rememberedLng: Double?): Triple<Double, Double, Double> =
        if (rememberedLat != null && rememberedLng != null &&
            rememberedLat in -90.0..90.0 && rememberedLng in -180.0..180.0 &&
            !(rememberedLat == 0.0 && rememberedLng == 0.0)
        ) {
            Triple(rememberedLat, rememberedLng, REMEMBERED_ZOOM)
        } else {
            Triple(DEFAULT_CENTER_LAT, DEFAULT_CENTER_LNG, NO_FIX_ZOOM)
        }

    /**
     * Ambient tile cache. Tiles already seen render instantly and survive
     * dead zones; 200 MB holds a city plus regular routes without bloating
     * storage on 16 GB head units.
     */
    const val AMBIENT_CACHE_BYTES = 200L * 1024 * 1024

    /** Puck/camera animation never runs faster than the map itself. */
    const val LOCATION_ANIMATION_MAX_FPS = 30

    /**
     * Layers hidden in lite mode (entry-level units): 3D buildings, the dense
     * low-rank POIs, one-way arrows and rail hatching cost frame time without
     * helping a driver. Ids are liberty's.
     */
    val LITE_HIDDEN_LAYERS: Set<String> = setOf(
        "building-3d", "poi_r20", "poi_r7",
        "road_one_way_arrow", "road_one_way_arrow_opposite",
        "road_major_rail_hatching", "road_transit_rail_hatching",
        "bridge_major_rail_hatching", "bridge_transit_rail_hatching",
        "tunnel_major_rail_hatching", "tunnel_transit_rail_hatching",
    )

    /**
     * Below walking speed GPS bearing is noise, so the camera stops rotating
     * with it (north-up) and only follows position.
     */
    fun rotateWithHeading(speedKmh: Int?): Boolean = (speedKmh ?: 0) >= HEADING_MIN_SPEED_KMH

    private const val HEADING_MIN_SPEED_KMH = 8
}
