package com.autoverse.launcher.feature.map

/**
 * Phase 7 prototype configuration. The style URL deliberately points at the
 * MapLibre demo server: dev/demo data, explicitly not a production tile
 * service. A productized map needs a licensed tile provider plus an offline
 * strategy; no API keys or secrets are used or stored anywhere.
 */
object MapPocConfig {
    const val STYLE_URL = "https://demotiles.maplibre.org/style.json"
    const val LOAD_TIMEOUT_MS = 15_000L
    const val START_LAT = 48.2082
    const val START_LON = 16.3738
    const val START_ZOOM = 11.0
    const val DEMO_HOST = "demotiles.maplibre.org"
}
