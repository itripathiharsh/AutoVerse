package com.autoverse.launcher.core.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedLocationTest {

    private fun point(s: SharedLocation?) = s as SharedLocation.Point
    private fun route(s: SharedLocation?) = s as SharedLocation.Route

    // ---- existing behaviour -------------------------------------------------

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

    // ---- directions: /maps/dir/ paths with data= -----------------------------

    @Test
    fun `dir url with two waypoints goes to the destination, not the map centre`() {
        val url = "https://www.google.com/maps/dir/Origin+Name/Destination+Name/@26.9,81.2,12z/data=" +
            "!3m1!4b1!4m14!4m13!1m5!1m1!1s0x399bfd991f32b16b:0x93ccba8909978be7!2m2!1d80.94!2d26.85" +
            "!1m5!1m1!1s0x399b0b1d:0x5f0c2a!2m2!1d81.20!2d26.92!3e0"
        assertEquals(SharedLocation.Point("Destination Name", 26.92, 81.20), parseSharedLocation(url))
    }

    @Test
    fun `dir waypoint coordinates are longitude first`() {
        val url = "https://www.google.com/maps/dir/A/B/@10.0,20.0,12z/data=!4m14!4m13" +
            "!1m5!1m1!1s0x1:0x1!2m2!1d77.2090!2d28.6139!1m5!1m1!1s0x2:0x2!2m2!1d72.8777!2d19.0760!3e0"
        val p = point(parseSharedLocation(url))
        assertEquals(19.0760, p.latitude, 1e-9)
        assertEquals(72.8777, p.longitude, 1e-9)
    }

    @Test
    fun `dir url with three waypoints keeps the middle one as a stop`() {
        val url = "https://www.google.com/maps/dir/Your+location/Via+Town/Final+City/@26.9,81.2,10z/data=" +
            "!4m18!4m17!1m3!2m2!1d80.90!2d26.80" +
            "!1m5!1m1!1s0xabc:0xdef!2m2!1d81.00!2d26.85" +
            "!1m5!1m1!1s0x1:0x2!2m2!1d81.20!2d26.92!3e0"
        assertEquals(
            SharedLocation.Route(
                stops = listOf(SharedPoint("Via Town", 26.85, 81.00)),
                destination = SharedPoint("Final City", 26.92, 81.20),
                near = 26.9 to 81.2,
            ),
            parseSharedLocation(url),
        )
    }

    @Test
    fun `dir url with empty origin and several stops keeps their order`() {
        val url = "https://www.google.com/maps/dir//Barabanki/Ayodhya/Gorakhpur/@26.8,82.0,8z/data=" +
            "!4m21!4m20!1m0" +
            "!1m5!1m1!1s0x1:0x1!2m2!1d81.18!2d26.92" +
            "!1m5!1m1!1s0x2:0x2!2m2!1d82.20!2d26.79" +
            "!1m5!1m1!1s0x3:0x3!2m2!1d83.37!2d26.76!3e0?entry=ttu"
        val r = route(parseSharedLocation(url))
        assertEquals(
            listOf(SharedPoint("Barabanki", 26.92, 81.18), SharedPoint("Ayodhya", 26.79, 82.20)),
            r.stops,
        )
        assertEquals(SharedPoint("Gorakhpur", 26.76, 83.37), r.destination)
    }

    @Test
    fun `dir data parser returns waypoints in order with null for your location`() {
        val data = "!3m1!4b1!4m13!4m12!1m0!1m5!1m1!1s0x1:0x2!2m2!1d81.2045!2d26.9312!1m3!2m2!1d80.5!2d26.5!3e0"
        // 4m12 holds: 1m0 (1) + 1m5 block (6) + 1m3 block (4) + 3e0 (1) = 12.
        assertEquals(
            listOf(null, 26.9312 to 81.2045, 26.5 to 80.5),
            directionsWaypointsFromData(data),
        )
    }

    @Test
    fun `dir data parser falls back to plain lng lat pairs`() {
        assertEquals(
            listOf(26.8 to 80.9, 26.9 to 81.2),
            directionsWaypointsFromData("!9z1!1d80.9!2d26.8!7b1!1d81.2!2d26.9"),
        )
    }

    @Test
    fun `dir url with only your location has no destination`() {
        assertNull(parseSharedLocation("https://www.google.com/maps/dir/Your+location/"))
    }

    @Test
    fun `dir url with empty origin and a named destination searches near the map`() {
        val r = route(parseSharedLocation("https://www.google.com/maps/dir//Barabanki/@26.9,81.2,12z"))
        assertEquals(emptyList<SharedPoint>(), r.stops)
        assertEquals(SharedPoint("Barabanki"), r.destination)
        assertNull("the map centre must not become the destination", r.destination.latitude)
        assertEquals(26.9 to 81.2, r.near)
    }

    @Test
    fun `dir path with plain names drops the origin`() {
        assertEquals(
            SharedLocation.Route(emptyList(), SharedPoint("Barabanki, Uttar Pradesh")),
            parseSharedLocation("https://www.google.com/maps/dir/Lucknow/Barabanki,+Uttar+Pradesh/"),
        )
        assertEquals(
            SharedLocation.Route(listOf(SharedPoint("Barabanki")), SharedPoint("Ayodhya")),
            parseSharedLocation("https://www.google.com/maps/dir/Lucknow/Barabanki/Ayodhya"),
        )
    }

    @Test
    fun `dir path with lat lng segments`() {
        assertEquals(
            SharedLocation.Point(null, 26.9268, 81.1834),
            parseSharedLocation("https://www.google.com/maps/dir/26.8467,80.9462/26.9268,81.1834/@26.88,81.05,11z"),
        )
        assertEquals(
            SharedLocation.Route(listOf(SharedPoint(null, 26.9268, 81.1834)), SharedPoint(null, 26.7922, 82.1998), 26.88 to 81.5),
            parseSharedLocation("https://www.google.com/maps/dir/26.8467,+80.9462/26.9268,81.1834/26.7922,82.1998/@26.88,81.5,9z"),
        )
    }

    @Test
    fun `directions share text names the destination`() {
        val s = parseSharedLocation("Directions to Ayodhya\nhttps://www.google.com/maps/dir/Lucknow/Ayodhya+Dham,+Uttar+Pradesh/")
        assertEquals(SharedLocation.Route(emptyList(), SharedPoint("Ayodhya")), s)
    }

    @Test
    fun `coordinatesFromMapsUrl gives the dir destination`() {
        val url = "https://www.google.com/maps/dir/A/B/@26.0,80.0,9z/data=!4m14!4m13" +
            "!1m5!1m1!1s0x1:0x1!2m2!1d80.94!2d26.85!1m5!1m1!1s0x2:0x2!2m2!1d81.20!2d26.92!3e0"
        assertEquals(26.92 to 81.20, coordinatesFromMapsUrl(url))
    }

    // ---- directions: api=1 and legacy saddr/daddr ---------------------------

    @Test
    fun `api=1 destination with waypoints`() {
        val url = "https://www.google.com/maps/dir/?api=1&origin=Lucknow&destination=Ayodhya" +
            "&waypoints=Barabanki%7C26.78,82.13&travelmode=driving"
        assertEquals(
            SharedLocation.Route(
                stops = listOf(SharedPoint("Barabanki"), SharedPoint(null, 26.78, 82.13)),
                destination = SharedPoint("Ayodhya"),
            ),
            parseSharedLocation(url),
        )
    }

    @Test
    fun `api=1 with coordinates and a plain pipe`() {
        val r = route(parseSharedLocation("https://www.google.com/maps/dir/?api=1&destination=26.79,82.19&waypoints=26.92,81.18|26.85,81.60"))
        assertEquals(listOf(SharedPoint(null, 26.92, 81.18), SharedPoint(null, 26.85, 81.60)), r.stops)
        assertEquals(SharedPoint(null, 26.79, 82.19), r.destination)
    }

    @Test
    fun `api=1 destination name only is found by name`() {
        assertEquals(
            SharedLocation.Route(emptyList(), SharedPoint("Hazratganj, Lucknow")),
            parseSharedLocation("https://www.google.com/maps/dir/?api=1&destination=Hazratganj%2C+Lucknow"),
        )
    }

    @Test
    fun `legacy saddr daddr with to stops`() {
        assertEquals(
            SharedLocation.Route(listOf(SharedPoint("Barabanki")), SharedPoint("Ayodhya")),
            parseSharedLocation("https://maps.google.com/maps?saddr=Lucknow&daddr=Barabanki+to:Ayodhya"),
        )
        assertEquals(
            SharedLocation.Point(null, 26.92, 81.20),
            parseSharedLocation("https://maps.google.com/maps?saddr=26.85,80.94&daddr=26.92,81.20"),
        )
        assertEquals(
            SharedLocation.Route(listOf(SharedPoint(null, 26.92, 81.18)), SharedPoint("Ayodhya", 26.79, 82.19)),
            parseSharedLocation("https://maps.google.com/?saddr=My+Location&daddr=26.92,81.18+to:Ayodhya@26.79,82.19"),
        )
    }

    // ---- places and searches ---------------------------------------------------

    @Test
    fun `place url with centre and pin never uses the centre`() {
        val url = "https://www.google.com/maps/place/Hanuman+Setu+Mandir/@26.8600,80.9100,14z/data=!4m6!3m5!1s0x0:0x1!8m2!3d26.8722!4d80.9315!16s%2Fg%2F11"
        val p = point(parseSharedLocation(url))
        assertEquals(26.8722, p.latitude, 1e-9)
        assertEquals(80.9315, p.longitude, 1e-9)
        assertEquals("Hanuman Setu Mandir", p.name)
    }

    @Test
    fun `place url without a pin uses the centre the place is shown at`() {
        assertEquals(
            SharedLocation.Point("Asandra", 26.93, 81.2),
            parseSharedLocation("https://www.google.com/maps/place/Asandra/@26.93,81.2,17z"),
        )
    }

    @Test
    fun `search url with a name searches near the map, not at its centre`() {
        assertEquals(
            SharedLocation.Query("India Gate", 28.6129 to 77.2295),
            parseSharedLocation("https://www.google.com/maps/search/India+Gate/@28.6129,77.2295,15z/data=!3m1!4b1"),
        )
        assertEquals(
            SharedLocation.Point(null, 26.9312, 81.2045),
            parseSharedLocation("https://www.google.com/maps/search/26.9312,+81.2045"),
        )
    }

    @Test
    fun `q with a name searches, q with ll is a point`() {
        assertEquals(SharedLocation.Query("Hazratganj"), parseSharedLocation("https://www.google.com/maps?q=Hazratganj"))
        assertEquals(
            SharedLocation.Point("India Gate", 28.61, 77.22),
            parseSharedLocation("https://maps.google.com/maps?q=India+Gate&ll=28.61,77.22"),
        )
        assertEquals(
            SharedLocation.Point("Office", 26.9, 81.2),
            parseSharedLocation("https://maps.google.com/?q=Office@26.9,81.2"),
        )
        assertEquals(
            SharedLocation.Point(null, 26.9, 81.2),
            parseSharedLocation("https://maps.google.com/?q=loc:26.9,81.2"),
        )
    }

    // ---- share text shapes ------------------------------------------------------

    @Test
    fun `directions to text with a short link names the destination`() {
        assertEquals(
            SharedLocation.ShortLink("Barabanki", "https://maps.app.goo.gl/Xy12AbC"),
            parseSharedLocation("Directions to Barabanki\nhttps://maps.app.goo.gl/Xy12AbC"),
        )
    }

    @Test
    fun `name and address text with a short link`() {
        assertEquals(
            SharedLocation.ShortLink("Hazratganj, Lucknow, Uttar Pradesh", "https://maps.app.goo.gl/Xy12AbC"),
            parseSharedLocation("Hazratganj, Lucknow, Uttar Pradesh\nhttps://maps.app.goo.gl/Xy12AbC"),
        )
        assertEquals(
            SharedLocation.ShortLink("Asandra", "https://maps.app.goo.gl/Xy12AbC"),
            parseSharedLocation("https://maps.app.goo.gl/Xy12AbC\nAsandra"),
        )
        assertEquals(
            SharedLocation.ShortLink(null, "https://goo.gl/maps/abc123"),
            parseSharedLocation("https://goo.gl/maps/abc123"),
        )
    }

    @Test
    fun `plain maps link has no name`() {
        assertEquals(SharedLocation.Point(null, 26.9312, 81.2045), parseSharedLocation("https://maps.google.com/?q=26.9312,81.2045"))
    }

    @Test
    fun `plain directions text without a link searches the place`() {
        assertEquals(SharedLocation.Query("Lucknow Junction"), parseSharedLocation("Directions to Lucknow Junction"))
    }

    @Test
    fun `share labels`() {
        assertEquals("X", shareLabel("Directions to X\nhttps://maps.app.goo.gl/a", "https://maps.app.goo.gl/a"))
        assertEquals("Café Lane, Delhi", shareLabel("Café Lane, Delhi https://maps.app.goo.gl/a", "https://maps.app.goo.gl/a"))
        assertNull(shareLabel("https://maps.app.goo.gl/a", "https://maps.app.goo.gl/a"))
    }

    @Test
    fun `point text shapes`() {
        assertEquals(SharedPoint(null, 26.9, 81.2), pointFromText("26.9, 81.2"))
        assertEquals(SharedPoint("Home", 26.9, 81.2), pointFromText("Home@26.9,81.2"))
        assertEquals(SharedPoint("Home", 26.9, 81.2), pointFromText("Home (26.9,81.2)"))
        assertEquals(SharedPoint("Home", 26.9, 81.2), pointFromText("26.9,81.2 (Home)"))
        assertEquals(SharedPoint("Lucknow"), pointFromText("Lucknow"))
        assertNull(pointFromText("Your location"))
        assertNull(pointFromText("  "))
    }

    // ---- redirect unwrapping ------------------------------------------------------

    @Test
    fun `consent continue is unwrapped`() {
        val consent = "https://consent.google.com/ml?continue=https%3A%2F%2Fwww.google.com%2Fmaps%2Fplace%2FAsandra%2F%4026.95%2C81.18%2C15z" +
            "%2Fdata%3D!3m1!4b1!4m6!3m5!1s0x0%3A0x0!8m2!3d26.9312!4d81.2045%3Fentry%3Dtts&gl=IN&m=0&pc=m&hl=en"
        assertEquals(
            "https://www.google.com/maps/place/Asandra/@26.95,81.18,15z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d26.9312!4d81.2045?entry=tts",
            unwrapRedirect(consent),
        )
        assertEquals(SharedLocation.Point("Asandra", 26.9312, 81.2045), parseSharedLocation(consent))
    }

    @Test
    fun `google url redirect and firebase link are unwrapped`() {
        assertEquals(
            "https://www.google.com/maps/@26.9,81.2,14z",
            unwrapRedirect("https://www.google.com/url?q=https://www.google.com/maps/@26.9,81.2,14z&sa=D"),
        )
        val fdl = "https://maps.app.goo.gl/?link=https%3A%2F%2Fwww.google.com%2Fmaps%2Fplace%2FAsandra%2F%4026.95%2C81.18%2C15z" +
            "%2Fdata%3D!3m1!4b1!4m6!3m5!1s0x0%3A0x0!8m2!3d26.9312!4d81.2045&apn=com.google.android.apps.maps"
        assertEquals(SharedLocation.Point("Asandra", 26.9312, 81.2045), parseSharedLocation(fdl))
    }

    @Test
    fun `intent redirects are unwrapped`() {
        assertEquals(
            "https://www.google.com/maps/place/X/@26.9,81.2,15z",
            unwrapRedirect(
                "intent://maps.app.goo.gl/abc#Intent;package=com.google.android.gms;scheme=https;" +
                    "S.browser_fallback_url=https%3A%2F%2Fwww.google.com%2Fmaps%2Fplace%2FX%2F%4026.9%2C81.2%2C15z;end",
            ),
        )
        assertEquals(
            "https://www.google.com/maps/place/X/@26.9,81.2,15z",
            unwrapRedirect("intent://www.google.com/maps/place/X/@26.9,81.2,15z#Intent;scheme=https;package=com.google.android.apps.maps;end"),
        )
    }

    @Test
    fun `final maps urls and plain short links are not unwrapped`() {
        assertNull(unwrapRedirect("https://www.google.com/maps/place/X/@26.9,81.2,15z?q=https://example.com"))
        assertNull(unwrapRedirect("https://maps.app.goo.gl/AbCdEf123"))
    }

    @Test
    fun `maps url and short link hosts`() {
        assertTrue(isGoogleMapsUrl("https://www.google.com/maps/place/X"))
        assertTrue(isGoogleMapsUrl("https://www.google.co.in/maps/dir/A/B"))
        assertTrue(isGoogleMapsUrl("https://maps.google.com/?q=1,2"))
        assertFalse(isGoogleMapsUrl("https://consent.google.com/ml?continue=x"))
        assertFalse(isGoogleMapsUrl("https://maps.app.goo.gl/x"))
        assertFalse(isGoogleMapsUrl("https://www.google.com/search?q=maps"))
        assertTrue(isShortLink("https://maps.app.goo.gl/x"))
        assertTrue(isShortLink("https://goo.gl/maps/x"))
        assertFalse(isShortLink("https://www.google.com/maps/place/X"))
    }

    // ---- HTML redirect pages --------------------------------------------------------

    @Test
    fun `html meta refresh is extracted`() {
        val html = """<html><head><meta http-equiv="refresh" content="0; url=https://www.google.com/maps/place/Asandra/@26.95,81.18,15z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d26.9312!4d81.2045?entry=tts&amp;g_ep=abc"></head><body></body></html>"""
        assertEquals(
            "https://www.google.com/maps/place/Asandra/@26.95,81.18,15z/data=!3m1!4b1!4m6!3m5!1s0x0:0x0!8m2!3d26.9312!4d81.2045?entry=tts&g_ep=abc",
            mapsUrlFromHtml(html),
        )
    }

    @Test
    fun `html og url is extracted and short link canonicals are skipped`() {
        val html = """<head><link rel="canonical" href="https://maps.app.goo.gl/xyz"><meta content='https://www.google.com/maps/dir/Lucknow/Ayodhya/' property='og:url'></head>"""
        assertEquals("https://www.google.com/maps/dir/Lucknow/Ayodhya/", mapsUrlFromHtml(html))
    }

    @Test
    fun `html with js escaped link skips the generic maps link`() {
        val html = """<a href="https://www.google.com/maps">Maps</a><script>window.APP_INITIALIZATION_STATE=[[["https:\/\/www.google.com\/maps\/dir\/\/Barabanki\/@26.9,81.2,12z\/data=!4m2!4m1!3e0"]]];</script>"""
        assertEquals("https://www.google.com/maps/dir//Barabanki/@26.9,81.2,12z/data=!4m2!4m1!3e0", mapsUrlFromHtml(html))
    }

    @Test
    fun `html js location redirect is extracted`() {
        val html = """<script>window.location.replace("https:\/\/www.google.com\/maps\/@26.93,81.2,14z&entry=tts");</script>"""
        assertEquals("https://www.google.com/maps/@26.93,81.2,14z&entry=tts", mapsUrlFromHtml(html))
    }

    @Test
    fun `html consent link is unwrapped`() {
        val html = """<a href="https://consent.google.com/ml?continue=https://www.google.com/maps/place/X/@26.9,81.2,15z&amp;gl=IN">Continue</a>"""
        assertEquals("https://www.google.com/maps/place/X/@26.9,81.2,15z", mapsUrlFromHtml(html))
    }

    @Test
    fun `html without a maps link gives nothing`() {
        assertNull(mapsUrlFromHtml("<html><body>Dynamic Link Not Found <a href=\"https://firebase.google.com\">x</a></body></html>"))
    }
}
