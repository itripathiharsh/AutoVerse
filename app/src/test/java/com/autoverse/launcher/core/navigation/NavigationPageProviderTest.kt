package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 18 — effective provider for the navigation page. A stored real-app
 * preference always wins; the system default is upgraded to Google Maps when
 * it is installed, because turn-by-turn is the page's primary job, while the
 * stale-preference marker is carried through unchanged.
 */
class NavigationPageProviderTest {

    private val providers = listOf<NavigationProvider>(
        NavigationProvider.SystemDefault,
        NavigationProvider.GoogleMaps,
        NavigationProvider.Waze,
    )

    @Test
    fun `a stored real-app preference wins`() {
        val resolved = ResolvedNavigationProvider(NavigationProvider.Waze, preferredMissing = false)
        val page = navigationPageProvider(resolved, providers)
        assertEquals(NavigationProvider.Waze, page.provider)
        assertFalse(page.preferredMissing)
    }

    @Test
    fun `a discovered open-only preference passes through`() {
        val openOnly = NavigationProvider.OpenOnly("com.example.nav", "Example Nav")
        val resolved = ResolvedNavigationProvider(openOnly, preferredMissing = false)
        val page = navigationPageProvider(resolved, providers)
        assertEquals(openOnly, page.provider)
    }

    @Test
    fun `the system default upgrades to Google Maps when installed`() {
        val resolved = ResolvedNavigationProvider(
            NavigationProvider.SystemDefault,
            preferredMissing = false,
        )
        val page = navigationPageProvider(resolved, providers)
        assertEquals(NavigationProvider.GoogleMaps, page.provider)
        assertFalse(page.preferredMissing)
    }

    @Test
    fun `the system default stays itself when Google Maps is absent`() {
        val resolved = ResolvedNavigationProvider(
            NavigationProvider.SystemDefault,
            preferredMissing = false,
        )
        val page = navigationPageProvider(resolved, listOf(NavigationProvider.SystemDefault))
        assertSame(NavigationProvider.SystemDefault, page.provider)
    }

    @Test
    fun `the stale-preference marker is carried through the upgrade`() {
        val resolved = ResolvedNavigationProvider(
            NavigationProvider.SystemDefault,
            preferredMissing = true,
        )
        val page = navigationPageProvider(resolved, providers)
        assertEquals(NavigationProvider.GoogleMaps, page.provider)
        assertTrue(page.preferredMissing)
    }

    @Test
    fun `an empty provider list keeps the system default`() {
        val resolved = ResolvedNavigationProvider(
            NavigationProvider.SystemDefault,
            preferredMissing = false,
        )
        val page = navigationPageProvider(resolved, emptyList())
        assertSame(NavigationProvider.SystemDefault, page.provider)
    }
}
