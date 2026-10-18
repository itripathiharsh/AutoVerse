package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 18 — canonical one-line destination text: the query verbatim, or
 * plain-decimal "lat, lng" for coordinates. The text must round-trip through
 * [parseNavigationDestination] back to the identical destination, because
 * saved places and recents persist as exactly this text.
 */
class NavigationDestinationTextTest {

    @Test
    fun `query text is kept verbatim`() {
        val text = navigationDestinationText(NavigationDestination.Query("Cafe Deluxe, Route 5"))
        assertEquals("Cafe Deluxe, Route 5", text)
    }

    @Test
    fun `coordinates render as plain decimal pairs`() {
        val text = navigationDestinationText(NavigationDestination.Coordinates(48.2082, 16.3738))
        assertEquals("48.2082, 16.3738", text)
    }

    @Test
    fun `negative and zero coordinates render without signs or exponents`() {
        assertEquals(
            "-33.8688, 151.2093",
            navigationDestinationText(NavigationDestination.Coordinates(-33.8688, 151.2093)),
        )
        assertEquals(
            "0, 0",
            navigationDestinationText(NavigationDestination.Coordinates(0.0, 0.0)),
        )
        assertEquals(
            "-0.0001, 2",
            navigationDestinationText(NavigationDestination.Coordinates(-1.0E-4, 2.0)),
        )
    }

    @Test
    fun `query text round-trips through the parser`() {
        val destination = NavigationDestination.Query("Stephansplatz 1")
        val parsed = parseNavigationDestination(navigationDestinationText(destination))
        assertEquals(DestinationParseResult.Valid(destination), parsed)
    }

    @Test
    fun `coordinate text round-trips through the parser`() {
        val destination = NavigationDestination.Coordinates(48.2082, 16.3738)
        val parsed = parseNavigationDestination(navigationDestinationText(destination))
        assertEquals(DestinationParseResult.Valid(destination), parsed)
    }

    @Test
    fun `whole-number coordinates round-trip as plain decimals`() {
        val destination = NavigationDestination.Coordinates(48.0, 16.0)
        val text = navigationDestinationText(destination)
        assertEquals("48, 16", text)
        assertEquals(DestinationParseResult.Valid(destination), parseNavigationDestination(text))
    }
}
