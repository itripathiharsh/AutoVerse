package com.autoverse.launcher.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 6 — preferred-provider resolution. A stored package that is missing
 * from the discovered list resolves to the system default with a stale
 * marker; null keeps the system default without noise.
 */
class NavigationProviderResolutionTest {

    private val providers = listOf<NavigationProvider>(
        NavigationProvider.SystemDefault,
        NavigationProvider.GoogleMaps,
        NavigationProvider.OpenOnly("com.example.nav", "Example Nav"),
    )

    @Test
    fun `no preference resolves to the system default`() {
        val resolved = resolveNavigationProvider(null, providers)
        assertEquals(NavigationProvider.SystemDefault, resolved.provider)
        assertFalse(resolved.preferredMissing)
    }

    @Test
    fun `installed preference resolves to that provider`() {
        val resolved = resolveNavigationProvider("com.waze", providers + NavigationProvider.Waze)
        assertEquals(NavigationProvider.Waze, resolved.provider)
        assertFalse(resolved.preferredMissing)
    }

    @Test
    fun `discovered open-only apps can be preferred`() {
        val resolved = resolveNavigationProvider("com.example.nav", providers)
        assertEquals(NavigationProvider.OpenOnly("com.example.nav", "Example Nav"), resolved.provider)
        assertFalse(resolved.preferredMissing)
    }

    @Test
    fun `missing preference keeps naming the preferred app and is flagged`() {
        // The label must name the app the launch will target; the launch
        // itself then reports NOT_INSTALLED instead of silently retargeting.
        val resolved = resolveNavigationProvider("com.waze", providers)
        assertEquals(NavigationProvider.Waze, resolved.provider)
        assertTrue(resolved.preferredMissing)
    }

    @Test
    fun `before discovery has run the preference is shown as-is, never downgraded`() {
        val resolved = resolveNavigationProvider(GOOGLE_MAPS_PACKAGE, emptyList())
        assertEquals(NavigationProvider.GoogleMaps, resolved.provider)
        assertFalse(resolved.preferredMissing)
    }
}
