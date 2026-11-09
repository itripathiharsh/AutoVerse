package com.autoverse.launcher.core.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedLocationTest {

    private fun point(s: SharedLocation?) = s as SharedLocation.Point

    @Test
    fun `google maps share with short link keeps the name`() {
        val s = parseSharedLocation("Asandra\nUttar Pradesh 225001\nhttps://maps.app.goo.gl/AbCdEf123")
        assertEquals(SharedLocation.ShortLink("Asandra", "https://maps.app.goo.gl/AbCdEf123"), s)
    }

    @Test
    fun `resolved place url uses the pin, not the map centre`() {
        val url = "https://www.google.com/maps/place/Asandra,+Uttar+Pradesh/@26.95,81.18,15z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d26.9312!4d81.2045!16s"
        val p = point(parseSharedLocation(url))
        assertEquals(26.9312, p.latitude, 1e-9)
        assertEquals(81.2045, p.longitude, 1e-9)
        assertEquals("Asandra, Uttar Pradesh", p.name)
    }

    @Test
    fun `dropped pin and query style links`() {
        point(parseSharedLocation("https://maps.google.com/?q=26.9312,81.2045")).let {
            assertEquals(26.9312, it.latitude, 1e-9)
        }
        point(parseSharedLocation("https://www.google.com/maps/search/?api=1&query=26.93%2C81.20")).let {
            assertEquals(81.20, it.longitude, 1e-9)
        }
        point(parseSharedLocation("https://www.google.com/maps/dir/?api=1&destination=26.5,80.9")).let {
            assertEquals(26.5, it.latitude, 1e-9)
        }
        point(parseSharedLocation("https://www.google.com/maps/@26.93,81.20,14z")).let {
            assertEquals(26.93, it.latitude, 1e-9)
        }
    }

    @Test
    fun `geo uris`() {
        assertEquals(SharedLocation.Point(null, 26.9, 81.2), parseGeoUri("geo:26.9,81.2"))
        assertEquals(SharedLocation.Point("Asandra", 26.9, 81.2), parseGeoUri("geo:0,0?q=26.9,81.2(Asandra)"))
        assertEquals(SharedLocation.Query("Asandra Barabanki"), parseGeoUri("geo:0,0?q=Asandra+Barabanki"))
        assertNull(parseGeoUri("geo:0,0"))
    }

    @Test
    fun `name-only links and plain text become a search`() {
        assertEquals(SharedLocation.Query("India Gate"), parseSharedLocation("https://www.google.com/maps/search/India+Gate"))
        assertEquals(SharedLocation.Query("Asandra Barabanki"), parseSharedLocation("Asandra Barabanki"))
        assertTrue(parseSharedLocation("26.93, 81.20") is SharedLocation.Point)
    }
}
