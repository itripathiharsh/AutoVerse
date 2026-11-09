package com.autoverse.launcher.core.permissions

import com.autoverse.launcher.core.map.MapStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarSetupTest {

    private fun status(
        precise: Boolean = true,
        serviceOn: Boolean = true,
        media: Boolean = true,
    ) = CarSetupStatus(
        preciseLocation = precise,
        anyLocation = precise,
        locationServiceOn = serviceOn,
        mediaAccess = media,
        phoneState = false,
        bluetooth = false,
        defaultLauncher = false,
    )

    @Test
    fun `all three required items make setup ready, optional ones never block`() {
        val ready = status()
        assertTrue(ready.allRequiredGranted)
        assertEquals(3, ready.requiredDone)
        assertEquals(3, ready.requiredTotal)
    }

    @Test
    fun `missing precise location, GPS switch or music control keeps setup locked`() {
        assertFalse(status(precise = false).allRequiredGranted)
        assertFalse(status(serviceOn = false).allRequiredGranted)
        assertFalse(status(media = false).allRequiredGranted)
        assertEquals(0, status(precise = false, serviceOn = false, media = false).requiredDone)
    }

    @Test
    fun `permission problem is reported before the location switch`() {
        assertEquals(LocationFix.GRANT, locationFixFor(hasPermission = false, serviceOn = false))
        assertEquals(LocationFix.TURN_ON, locationFixFor(hasPermission = true, serviceOn = false))
        assertNull(locationFixFor(hasPermission = true, serviceOn = true))
    }

    @Test
    fun `map opens at the remembered car position`() {
        val (lat, lng, zoom) = MapStyle.startCamera(28.6139, 77.2090)
        assertEquals(28.6139, lat, 1e-9)
        assertEquals(77.2090, lng, 1e-9)
        assertEquals(MapStyle.REMEMBERED_ZOOM, zoom, 1e-9)
    }

    @Test
    fun `map never opens in the ocean at 0,0`() {
        listOf(null to null, 0.0 to 0.0, 999.0 to 77.0, 28.0 to null).forEach { (lat, lng) ->
            val (cLat, cLng, zoom) = MapStyle.startCamera(lat, lng)
            assertEquals(MapStyle.DEFAULT_CENTER_LAT, cLat, 1e-9)
            assertEquals(MapStyle.DEFAULT_CENTER_LNG, cLng, 1e-9)
            assertEquals(MapStyle.NO_FIX_ZOOM, zoom, 1e-9)
        }
    }
}
