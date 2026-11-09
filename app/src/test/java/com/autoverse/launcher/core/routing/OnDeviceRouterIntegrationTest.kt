package com.autoverse.launcher.core.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Real BRouter run on real Indian road data. Needs the Delhi tile
 * (E75_N25.rd5, ~48 MB) in the folder named by the AUTOVERSE_ROUTING_TILES
 * environment variable; skipped otherwise so CI without the data stays green.
 */
class OnDeviceRouterIntegrationTest {

    @Test
    fun `routes across Delhi on the head-unit engine`() {
        val dir = System.getenv("AUTOVERSE_ROUTING_TILES")?.let(::File)
        assumeTrue("set AUTOVERSE_ROUTING_TILES to a folder with E75_N25.rd5", dir != null && File(dir, "E75_N25.rd5").exists())
        val profile = File("src/main/assets/brouter/car-vario.brf").absoluteFile
        // BRouter reads lookups.dat from the profile's folder.
        val router = OnDeviceRouter(dir!!) { profile.path }

        val connaughtPlace = GeoPoint(28.6315, 77.2167)
        val indiaGate = GeoPoint(28.6129, 77.2295)
        val started = System.currentTimeMillis()
        val route = runBlocking { router.route(connaughtPlace, indiaGate) }
        val tookMs = System.currentTimeMillis() - started

        println("CP -> India Gate: ${formatDistance(route.totalMeters)}, ${formatDuration(route.totalSeconds)}, " +
            "${route.points.size} points, ${route.maneuvers.size} instructions, computed in $tookMs ms")
        route.maneuvers.take(8).forEach { println("  - ${maneuverText(it)} at ${formatDistance(route.cumulativeMeters[it.pointIndex])}") }

        assertTrue("route length plausible", route.totalMeters in 2_000.0..8_000.0)
        assertTrue("has turn instructions", route.maneuvers.size >= 2)
        assertTrue("ETA plausible", route.totalSeconds in 120.0..1_800.0)

        // A longer cross-city route: Connaught Place -> IGI Airport T3.
        val t3 = GeoPoint(28.5562, 77.1000)
        val s2 = System.currentTimeMillis()
        val longRoute = runBlocking { router.route(connaughtPlace, t3) }
        println("CP -> IGI T3: ${formatDistance(longRoute.totalMeters)}, ${formatDuration(longRoute.totalSeconds)}, " +
            "computed in ${System.currentTimeMillis() - s2} ms")
        assertTrue(longRoute.totalMeters in 12_000.0..30_000.0)
    }
}
