package com.autoverse.launcher.core.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTest {

    // A straight 1 km road heading east, then a left turn going north 500 m.
    private val start = GeoPoint(28.6000, 77.2000)
    private val corner = GeoPoint(28.6000, 77.21023) // ~1000 m east at this latitude
    private val end = GeoPoint(28.60450, 77.21023) // ~500 m north
    private val route = Route.of(
        points = listOf(start, corner, end),
        times = listOf(0.0, 90.0, 140.0),
        maneuvers = listOf(Maneuver(1, ManeuverType.TURN_LEFT)),
    )

    @Test
    fun `route measures distance and adds an arrival instruction`() {
        assertEquals(1500.0, route.totalMeters, 15.0)
        assertEquals(140.0, route.totalSeconds, 1e-9)
        assertEquals(ManeuverType.ARRIVE, route.maneuvers.last().type)
        assertEquals(2, route.maneuvers.size)
    }

    @Test
    fun `missing router times fall back to a city speed so ETA is never blank`() {
        val r = Route.of(listOf(start, corner), times = null, maneuvers = emptyList())
        assertEquals(r.totalMeters / Route.FALLBACK_SPEED_MPS, r.totalSeconds, 1e-6)
    }

    @Test
    fun `progress halfway along the first road`() {
        val car = GeoPoint(28.60003, 77.20512) // ~500 m east, 3 m off the line
        val p = computeProgress(route, car)
        assertEquals(500.0, p.traveledMeters, 10.0)
        assertEquals(1000.0, p.remainingMeters, 15.0)
        assertEquals(ManeuverType.TURN_LEFT, p.nextManeuver?.type)
        assertEquals(500.0, p.distanceToNextManeuver, 10.0)
        assertTrue(p.offRouteMeters < 10.0)
        assertEquals(140.0 - 45.0, p.remainingSeconds, 3.0)
        assertFalse(p.arrived)
    }

    @Test
    fun `after the turn the next instruction is arrival`() {
        val car = GeoPoint(28.6020, 77.21023)
        val p = computeProgress(route, car, hintSegment = 0)
        assertEquals(ManeuverType.ARRIVE, p.nextManeuver?.type)
        assertEquals(1, p.segmentIndex)
    }

    @Test
    fun `car 100 m away from the road is off route`() {
        val car = GeoPoint(28.6009, 77.2050)
        val p = computeProgress(route, car)
        assertTrue(p.offRouteMeters > OFF_ROUTE_METERS)
    }

    @Test
    fun `arrival is detected near the end`() {
        val p = computeProgress(route, GeoPoint(28.60440, 77.21023), hintSegment = 1)
        assertTrue(p.arrived)
    }

    @Test
    fun `then-instruction only shown when the next turn follows closely`() {
        val r = Route.of(
            listOf(start, corner, end),
            null,
            listOf(Maneuver(1, ManeuverType.TURN_LEFT)),
        )
        // ARRIVE is 500 m after the turn: too far for "then".
        assertNull(computeProgress(r, start).thenManeuver)
    }

    @Test
    fun `tile names follow BRouter 5 degree south-west corner naming`() {
        assertEquals("E75_N25", routingTileName(28.61, 77.21)) // Delhi
        assertEquals("E70_N15", routingTileName(19.07, 72.88)) // Mumbai
        assertEquals("E75_N10", routingTileName(12.97, 77.59)) // Bengaluru
        assertEquals("W5_S5", routingTileName(-0.5, -0.5))
    }

    @Test
    fun `tiles between two cities cover the route corridor`() {
        val delhi = GeoPoint(28.61, 77.21)
        val jaipur = GeoPoint(26.91, 75.79)
        val ahmedabad = GeoPoint(23.02, 72.57)
        // Delhi -> Jaipur stays inside one 5° tile.
        assertEquals(listOf("E75_N25"), routingTilesBetween(delhi, jaipur))
        assertEquals(
            setOf("E70_N20", "E75_N20", "E70_N25", "E75_N25"),
            routingTilesBetween(delhi, ahmedabad).toSet(),
        )
    }

    @Test
    fun `distances and durations read like a driver says them`() {
        assertEquals("350 m", formatDistance(347.0))
        assertEquals("1.2 km", formatDistance(1234.0))
        assertEquals("12 km", formatDistance(12_400.0))
        assertEquals("1 h 25 min", formatDuration(85 * 60.0))
        assertEquals("1 min", formatDuration(10.0))
        assertEquals("500 metres", spokenDistance(489.0))
        assertEquals("1.5 kilometres", spokenDistance(1500.0))
        assertEquals("1 kilometre", spokenDistance(1000.0))
    }

    @Test
    fun `left-hand-traffic roundabout exits are spoken as positive numbers`() {
        assertEquals(2, speakableRoundaboutExit(-2))
        assertEquals(1, speakableRoundaboutExit(1))
        assertEquals(0, speakableRoundaboutExit(-28)) // Connaught Place: no "exit 28"
        assertEquals(0, speakableRoundaboutExit(0))
    }

    @Test
    fun `speed limit is read from way tags`() {
        assertEquals(50, maxspeedFromTags("highway=primary maxspeed=50 surface=asphalt"))
        assertEquals(0, maxspeedFromTags("highway=primary maxspeed=none"))
        assertEquals(0, maxspeedFromTags("highway=residential"))
        assertEquals(80, maxspeedFromTags("maxspeed=80"))
    }

    @Test
    fun `over-limit ignores GPS jitter at the limit`() {
        assertFalse(isOverLimit(52, 50))
        assertTrue(isOverLimit(55, 50))
        assertFalse(isOverLimit(90, null))
    }

    @Test
    fun `route messages give per-point limits and cameras`() {
        val json = """
            {"features":[{"properties":{"messages":[
              ["Longitude","Latitude","Elevation","Distance","CostPerKm","ElevCost","TurnCost","NodeCost","InitialCost","WayTags","NodeTags","Time","Energy"],
              ["77210230","28600000","0","1000","1000","0","0","0","0","highway=primary maxspeed=60","highway=speed_camera","90","0"],
              ["77210230","28604500","0","500","1000","0","0","0","0","highway=residential maxspeed=30","","140","0"]
             ]},
             "geometry":{"coordinates":[[77.2,28.6],[77.21023,28.6],[77.21023,28.6045]]}}]}
        """.trimIndent()
        val r = parseBRouterJson(json)
        assertEquals(listOf(60, 60, 30), r.speedLimits.toList())
        assertEquals(listOf(1), r.cameras)
        val p = computeProgress(r, GeoPoint(28.60003, 77.20512))
        assertEquals(60, p.speedLimitKmh)
        assertEquals(500.0, p.nextCameraMeters!!, 15.0)
    }

    @Test
    fun `instructions are plain English`() {
        assertEquals("Turn left", maneuverText(Maneuver(0, ManeuverType.TURN_LEFT)))
        assertEquals("At the roundabout, take exit 2", maneuverText(Maneuver(0, ManeuverType.ROUNDABOUT, 2)))
    }

    @Test
    fun `voice prompts come earlier at highway speed`() {
        assertEquals(PromptStage.FAR, promptStageFor(350.0, 40))
        assertEquals(PromptStage.NEAR, promptStageFor(60.0, 40))
        assertNull(promptStageFor(900.0, 40))
        assertEquals(PromptStage.FAR, promptStageFor(900.0, 90))
        assertEquals(PromptStage.NEAR, promptStageFor(200.0, 90))
    }

    @Test
    fun `photon results become named places with a locality line`() {
        val json = """
            {"features":[
              {"geometry":{"coordinates":[77.2295,28.6129]},
               "properties":{"name":"India Gate","city":"New Delhi","state":"Delhi"}},
              {"geometry":{"coordinates":[77.32,28.57]},
               "properties":{"street":"Sector 18 Road","housenumber":"12","city":"Noida","state":"Uttar Pradesh"}},
              {"geometry":{"coordinates":[77.32,28.57]},"properties":{}}
            ]}
        """.trimIndent()
        val places = parsePhotonResults(json)
        assertEquals(2, places.size)
        assertEquals("India Gate", places[0].name)
        assertEquals("New Delhi, Delhi", places[0].detail)
        assertEquals(28.6129, places[0].latitude, 1e-9)
        assertEquals("12 Sector 18 Road", places[1].name)
    }

    @Test
    fun `search url is limited to India and biased to the car`() {
        val url = photonSearchUrl("https://photon.komoot.io", "india gate", GeoPoint(28.61349, 77.20871), 5)
        assertTrue(url.startsWith("https://photon.komoot.io/api/?q=india+gate&limit=5"))
        assertTrue(url.contains("bbox=68.0,6.5,97.5,37.5"))
        // Position rounded to ~1 km before it leaves the unit.
        assertTrue(url, url.endsWith("&lat=28.61&lon=77.21"))
    }

    @Test
    fun `parses BRouter geojson with times and voice hints`() {
        val json = """
            {"type":"FeatureCollection","features":[{"type":"Feature",
             "properties":{"track-length":"1500","total-time":"140",
               "voicehints":[[1,2,0,500.0,-90,"x"],[1,16,0,0.0,0]],
               "times":[0,90,140]},
             "geometry":{"type":"LineString","coordinates":[
               [77.2,28.6,210],[77.21023,28.6,211],[77.21023,28.6045,212]]}}]}
        """.trimIndent()
        val r = parseBRouterJson(json)
        assertEquals(3, r.points.size)
        assertEquals(28.6045, r.points[2].latitude, 1e-9)
        assertEquals(140.0, r.totalSeconds, 1e-9)
        // Beeline hint (16) is dropped; the left turn and arrival remain.
        assertEquals(listOf(ManeuverType.TURN_LEFT, ManeuverType.ARRIVE), r.maneuvers.map { it.type })
        assertNotNull(r.maneuvers.first())
    }
}
