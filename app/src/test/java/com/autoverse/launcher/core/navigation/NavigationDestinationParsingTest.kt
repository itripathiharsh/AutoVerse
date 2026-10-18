package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase 6 — destination parsing. Raw input becomes a query unless it is
 * exactly two plain-decimal numbers in valid latitude/longitude ranges;
 * scientific notation stays a query, out-of-range numbers are rejected and
 * blank input never reaches a provider.
 */
class NavigationDestinationParsingTest {

    @Test
    fun `blank input parses to Empty`() {
        assertEquals(DestinationParseResult.Empty, parseNavigationDestination(""))
        assertEquals(DestinationParseResult.Empty, parseNavigationDestination("   "))
        assertEquals(DestinationParseResult.Empty, parseNavigationDestination("\t\n"))
    }

    @Test
    fun `plain text becomes a trimmed query`() {
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Query("Wembley Stadium")),
            parseNavigationDestination("  Wembley Stadium  "),
        )
    }

    @Test
    fun `addresses with commas stay queries`() {
        assertEquals(
            DestinationParseResult.Valid(
                NavigationDestination.Query("Wembley, London, UK"),
            ),
            parseNavigationDestination("Wembley, London, UK"),
        )
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Query("1, 2, 3")),
            parseNavigationDestination("1, 2, 3"),
        )
    }

    @Test
    fun `two decimal numbers become coordinates`() {
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Coordinates(51.5074, -0.1278)),
            parseNavigationDestination("51.5074,-0.1278"),
        )
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Coordinates(51.5074, -0.1278)),
            parseNavigationDestination("51.5074, -0.1278"),
        )
    }

    @Test
    fun `integer and sign forms stay coordinates`() {
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Coordinates(1.0, 2.0)),
            parseNavigationDestination("1,2"),
        )
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Coordinates(-33.86, 151.2)),
            parseNavigationDestination("-33.86, +151.2"),
        )
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Coordinates(0.5, -0.5)),
            parseNavigationDestination(".5, -0.5"),
        )
    }

    @Test
    fun `range boundaries are inclusive`() {
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Coordinates(-90.0, -180.0)),
            parseNavigationDestination("-90, -180"),
        )
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Coordinates(90.0, 180.0)),
            parseNavigationDestination("90, 180"),
        )
    }

    @Test
    fun `out-of-range numbers are rejected`() {
        assertEquals(
            DestinationParseResult.Invalid(DestinationParseFailure.OUT_OF_RANGE),
            parseNavigationDestination("90.1, 0"),
        )
        assertEquals(
            DestinationParseResult.Invalid(DestinationParseFailure.OUT_OF_RANGE),
            parseNavigationDestination("0, 180.001"),
        )
    }

    @Test
    fun `scientific notation is not treated as coordinates`() {
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Query("1e3, 2e3")),
            parseNavigationDestination("1e3, 2e3"),
        )
    }

    @Test
    fun `overlong input is rejected instead of truncated`() {
        assertEquals(
            DestinationParseResult.Valid(NavigationDestination.Query("a".repeat(512))),
            parseNavigationDestination("a".repeat(512)),
        )
        assertEquals(
            DestinationParseResult.Invalid(DestinationParseFailure.TOO_LONG),
            parseNavigationDestination("a".repeat(513)),
        )
    }

    @Test
    fun `coordinate formatting is plain decimal without trailing zeros`() {
        assertEquals("51.5074", formatCoordinate(51.5074))
        assertEquals("51", formatCoordinate(51.0))
        assertEquals("-0.1278", formatCoordinate(-0.1278))
        assertEquals("0.0001", formatCoordinate(0.0001))
        assertEquals("0.0000001", formatCoordinate(1.0E-7))
    }

    @Test
    fun `negative zero formats as zero`() {
        assertEquals("0", formatCoordinate(-0.0))
        assertEquals("0", formatCoordinate(0.0))
    }
}
