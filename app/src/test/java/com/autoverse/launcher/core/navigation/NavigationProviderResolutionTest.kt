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
    fun `missing preference falls back to system default and is flagged`() {
        val resolved = resolveNavigationProvider("com.waze", providers)
        assertEquals(NavigationProvider.SystemDefault, resolved.provider)
        assertTrue(resolved.preferredMissing)
    }
}
