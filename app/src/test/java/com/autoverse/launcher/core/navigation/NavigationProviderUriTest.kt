package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase 6 — per-provider URI construction. Each provider keeps its own
 * documented format (Google Maps intents guide, Waze deep-link guide,
 * platform geo: scheme); open-only providers never produce a destination URI.
 */
class NavigationProviderUriTest {

    private val london = NavigationDestination.Coordinates(51.5074, -0.1278)
    private val trafalgar = NavigationDestination.Query("Trafalgar Square")

    @Test
    fun `google maps coordinates use google navigation scheme`() {
        assertEquals(
            "google.navigation:q=51.5074,-0.1278",
            googleMapsDestinationUri(london),
        )
    }

    @Test
    fun `google maps query is percent encoded`() {
        assertEquals(
            "google.navigation:q=Trafalgar%20Square",
            googleMapsDestinationUri(trafalgar),
        )
        assertEquals(
            "google.navigation:q=Kantstra%C3%9Fe%2012",
            googleMapsDestinationUri(NavigationDestination.Query("Kantstraße 12")),
        )
    }

    @Test
    fun `waze coordinates use ll and navigate yes`() {
        assertEquals(
            "waze://?ll=51.5074,-0.1278&navigate=yes",
            wazeDestinationUri(london),
        )
    }

    @Test
    fun `waze query uses q and navigate yes`() {
        assertEquals(
            "waze://?q=Trafalgar%20Square&navigate=yes",
            wazeDestinationUri(trafalgar),
        )
    }

    @Test
    fun `geo coordinates keep position and matching marker`() {
        assertEquals(
            "geo:51.5074,-0.1278?q=51.5074,-0.1278",
            geoDestinationUri(london),
        )
    }

    @Test
    fun `geo query uses the zero-zero search form`() {
        assertEquals(
            "geo:0,0?q=Wembley%20Stadium",
            geoDestinationUri(NavigationDestination.Query("Wembley Stadium")),
        )
    }

    @Test
    fun `coordinate formatting drops noise inside uris`() {
        assertEquals(
            "google.navigation:q=51,0",
            googleMapsDestinationUri(NavigationDestination.Coordinates(51.0, -0.0)),
        )
    }

    @Test
    fun `open only providers never produce a destination uri`() {
        assertNull(navigationDestinationUri(NavigationProvider.OpenOnly("com.example.nav", "Nav"), london))
        assertNull(navigationDestinationUri(NavigationProvider.OpenOnly("com.example.nav", "Nav"), trafalgar))
    }

    @Test
    fun `registered providers map to their documented format`() {
        assertEquals(
            geoDestinationUri(london),
            navigationDestinationUri(NavigationProvider.SystemDefault, london),
        )
        assertEquals(
            googleMapsDestinationUri(london),
            navigationDestinationUri(NavigationProvider.GoogleMaps, london),
        )
        assertEquals(
            wazeDestinationUri(london),
            navigationDestinationUri(NavigationProvider.Waze, london),
        )
    }

    @Test
    fun `capability flags match the documented formats`() {
        assertEquals(false, NavigationProvider.SystemDefault.supportsOpenApp)
        assertEquals(true, NavigationProvider.SystemDefault.supportsDestination)
        assertEquals("com.google.android.apps.maps", NavigationProvider.GoogleMaps.packageName)
        assertEquals(true, NavigationProvider.GoogleMaps.supportsDestination)
        assertEquals("com.waze", NavigationProvider.Waze.packageName)
        assertEquals(true, NavigationProvider.Waze.supportsDestination)
        assertEquals(true, NavigationProvider.OpenOnly("com.example.nav", "Nav").supportsOpenApp)
        assertEquals(false, NavigationProvider.OpenOnly("com.example.nav", "Nav").supportsDestination)
    }
}
