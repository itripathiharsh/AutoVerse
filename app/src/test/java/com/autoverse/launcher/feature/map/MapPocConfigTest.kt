package com.autoverse.launcher.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/**
 * Phase 7 — policy guard for the prototype tile endpoint: HTTPS only, the
 * MapLibre demo host, and explicitly never a public OSM community tile
 * server. Also pins the load-timeout into a sane budget.
 */
class MapPocConfigTest {

    @Test
    fun `style URL is https on the MapLibre demo host, never an OSM tile server`() {
        val uri = URI(MapPocConfig.STYLE_URL)
        assertEquals("https", uri.scheme)
        assertEquals(MapPocConfig.DEMO_HOST, uri.host)
        assertTrue(MapPocConfig.STYLE_URL.endsWith("/style.json"))
    }

    @Test
    fun `load timeout is a bounded positive budget`() {
        assertTrue(MapPocConfig.LOAD_TIMEOUT_MS in 5_000..60_000)
    }
}
