package com.autoverse.launcher.core.navigation

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 0.28.2 — the Google Maps launch fix. These cover the decision made for
 * every tap before any intent is fired: which provider, which URI, which
 * package, which flags, and which precise failure.
 */
class NavigationLaunchPlanTest {

    private val museum = NavigationDestination.Query("Sample Museum & Café, Vienna")

    @Test
    fun `stored package maps to its provider without the discovery list`() {
        assertEquals(NavigationProvider.SystemDefault, providerForPackage(null))
        assertEquals(NavigationProvider.GoogleMaps, providerForPackage(GOOGLE_MAPS_PACKAGE))
        assertEquals(NavigationProvider.Waze, providerForPackage(WAZE_PACKAGE))
        assertEquals(
            NavigationProvider.OpenOnly("com.example.nav", "com.example.nav"),
            providerForPackage("com.example.nav"),
        )
    }

    @Test
    fun `open Google Maps uses its own launcher activity in a new task`() {
        val plan = planNavigationLaunch(NavigationProvider.GoogleMaps, null, PackageState.ENABLED)
        assertEquals(NavigationLaunchPlan.OpenApp(GOOGLE_MAPS_PACKAGE), plan)
        val flags = (plan as NavigationLaunchPlan.OpenApp).flags
        assertTrue(flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        // Brings an already-running Maps task forward instead of stacking a copy.
        assertTrue(flags and Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED != 0)
    }

    @Test
    fun `missing Google Maps is reported as not installed, not as a generic failure`() {
        assertEquals(
            NavigationLaunchPlan.Rejected(NavigationOutcome.NOT_INSTALLED),
            planNavigationLaunch(NavigationProvider.GoogleMaps, null, PackageState.MISSING),
        )
        assertEquals(
            NavigationLaunchPlan.Rejected(NavigationOutcome.NOT_INSTALLED),
            planNavigationLaunch(NavigationProvider.GoogleMaps, museum, PackageState.MISSING),
        )
    }

    @Test
    fun `disabled Google Maps is reported as disabled`() {
        assertEquals(
            NavigationLaunchPlan.Rejected(NavigationOutcome.APP_DISABLED),
            planNavigationLaunch(NavigationProvider.GoogleMaps, null, PackageState.DISABLED),
        )
    }

    @Test
    fun `destination goes to Maps turn-by-turn first, then Maps geo search, both pinned`() {
        val plan = planNavigationLaunch(NavigationProvider.GoogleMaps, museum, PackageState.ENABLED)
        val attempts = (plan as NavigationLaunchPlan.View).attempts
        assertEquals(
            listOf(
                ViewIntentSpec("google.navigation:q=Sample%20Museum%20%26%20Caf%C3%A9%2C%20Vienna", GOOGLE_MAPS_PACKAGE),
                ViewIntentSpec("geo:0,0?q=Sample%20Museum%20%26%20Caf%C3%A9%2C%20Vienna", GOOGLE_MAPS_PACKAGE),
            ),
            attempts,
        )
    }

    @Test
    fun `coordinates are passed as numbers, never as a text query`() {
        val plan = planNavigationLaunch(
            NavigationProvider.GoogleMaps,
            NavigationDestination.Coordinates(48.2082, 16.3738),
            PackageState.ENABLED,
        )
        assertEquals(
            "google.navigation:q=48.2082,16.3738",
            (plan as NavigationLaunchPlan.View).attempts.first().uri,
        )
    }

    @Test
    fun `blank destination is rejected before anything is launched`() {
        assertEquals(
            NavigationLaunchPlan.Rejected(NavigationOutcome.INVALID_DESTINATION),
            planNavigationLaunch(NavigationProvider.GoogleMaps, NavigationDestination.Query("   "), PackageState.ENABLED),
        )
    }

    @Test
    fun `open-only apps can be opened but never receive destinations`() {
        val provider = NavigationProvider.OpenOnly("com.example.nav", "Example Nav")
        assertEquals(NavigationLaunchPlan.OpenApp("com.example.nav"), planNavigationLaunch(provider, null, PackageState.ENABLED))
        assertEquals(
            NavigationLaunchPlan.Rejected(NavigationOutcome.NOT_SUPPORTED),
            planNavigationLaunch(provider, museum, PackageState.ENABLED),
        )
    }

    @Test
    fun `system default leaves the choice to Android`() {
        val open = planNavigationLaunch(NavigationProvider.SystemDefault, null, PackageState.ENABLED)
        assertEquals(NavigationLaunchPlan.View(listOf(ViewIntentSpec("geo:0,0", null))), open)
        val navigate = planNavigationLaunch(NavigationProvider.SystemDefault, museum, PackageState.ENABLED)
        assertEquals(null, (navigate as NavigationLaunchPlan.View).attempts.single().packageName)
    }

    @Test
    fun `waze destinations use the documented deep link pinned to waze`() {
        val plan = planNavigationLaunch(NavigationProvider.Waze, museum, PackageState.ENABLED)
        val attempt = (plan as NavigationLaunchPlan.View).attempts.single()
        assertEquals(WAZE_PACKAGE, attempt.packageName)
        assertTrue(attempt.uri.startsWith("waze://?q=") && attempt.uri.endsWith("&navigate=yes"))
    }

    @Test
    fun `maps status comes from discovery and is unknown before it runs`() {
        assertEquals(MapsStatus.UNKNOWN, mapsStatusOf(emptyList()))
        assertEquals(
            MapsStatus.INSTALLED,
            mapsStatusOf(listOf(NavigationProvider.SystemDefault, NavigationProvider.GoogleMaps)),
        )
        assertEquals(MapsStatus.NOT_INSTALLED, mapsStatusOf(listOf(NavigationProvider.SystemDefault)))
    }
}
