package com.autoverse.launcher.core.preferences

import com.autoverse.launcher.core.navigation.NavigationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationPreferenceMappingTest {

    private val mapsPackage = requireNotNull(NavigationProvider.GoogleMaps.packageName)

    @Test
    fun `absent key means the app default - never a package-less system default`() {
        assertEquals(mapsPackage, navigationPackageFromStorage(null, mapsPackage))
    }

    @Test
    fun `absent key with no default stays null`() {
        assertNull(navigationPackageFromStorage(null, null))
    }

    @Test
    fun `explicit system-default marker decodes to null`() {
        assertNull(navigationPackageFromStorage(NAVIGATION_SYSTEM_DEFAULT_MARKER, mapsPackage))
    }

    @Test
    fun `stored package decodes unchanged`() {
        assertEquals(
            "com.waze",
            navigationPackageFromStorage("com.waze", mapsPackage),
        )
    }

    @Test
    fun `null encodes to the system-default marker`() {
        assertEquals("", navigationPackageToStorage(null))
    }

    @Test
    fun `package encodes unchanged and round-trips`() {
        assertEquals("com.waze", navigationPackageToStorage("com.waze"))
        assertEquals(
            "com.waze",
            navigationPackageFromStorage(navigationPackageToStorage("com.waze"), mapsPackage),
        )
    }

    @Test
    fun `blank package names encode to the marker instead of junk`() {
        assertEquals("", navigationPackageToStorage("   "))
    }
}
