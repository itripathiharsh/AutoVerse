package com.autoverse.launcher.core.map

import com.autoverse.launcher.feature.diagnostics.mapRenderReadingText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMapLogicTest {

    @Test
    fun `styles come from keyless OpenFreeMap and follow day and night`() {
        assertEquals("https://tiles.openfreemap.org/styles/dark", MapStyle.styleUrl(night = true))
        assertEquals("https://tiles.openfreemap.org/styles/positron", MapStyle.styleUrl(night = false))
        listOf(true, false).forEach { night ->
            val url = MapStyle.styleUrl(night)
            assertTrue(url.startsWith("https://"))
            assertFalse("no API key may ever be embedded", url.contains("key=", ignoreCase = true))
        }
    }

    @Test
    fun `OpenStreetMap attribution required by the ODbL is present`() {
        assertTrue(MapStyle.ATTRIBUTION.contains("OpenStreetMap contributors"))
    }

    @Test
    fun `camera rotates with heading only at driving speed`() {
        assertFalse(MapStyle.rotateWithHeading(null))
        assertFalse(MapStyle.rotateWithHeading(0))
        assertFalse(MapStyle.rotateWithHeading(7))
        assertTrue(MapStyle.rotateWithHeading(8))
        assertTrue(MapStyle.rotateWithHeading(90))
    }

    @Test
    fun `parked car with GPS jitter does not trigger redraws`() {
        assertFalse(isMeaningfulMove(distanceMeters = 0.4f, bearingDelta = 1f))
        assertTrue(isMeaningfulMove(distanceMeters = 2f, bearingDelta = 0f))
        assertTrue(isMeaningfulMove(distanceMeters = 0f, bearingDelta = 5f))
    }

    @Test
    fun `bearing difference wraps around north`() {
        assertEquals(2f, bearingDelta(359f, 1f), 0.001f)
        assertEquals(180f, bearingDelta(0f, 180f), 0.001f)
        assertEquals(10f, bearingDelta(100f, 90f), 0.001f)
    }

    @Test
    fun `steady 30 fps window is smooth with no slow frames`() {
        val acc = MapFrameAccumulator()
        var snapshot: MapRenderSnapshot? = null
        var t = 0L
        while (snapshot == null) {
            snapshot = acc.onFrame(t)
            t += 33
        }
        assertEquals(30.3, snapshot.activeFps, 0.5)
        assertEquals(0, snapshot.slowFrames)
        assertTrue(snapshot.meetsTarget)
    }

    @Test
    fun `a hitch is counted as a slow frame and fails the target`() {
        val acc = MapFrameAccumulator()
        val times = listOf(0L, 33, 66, 166, 199, 232) + (265L..1300L step 33)
        val snapshot = times.firstNotNullOf { acc.onFrame(it) }
        assertEquals(1, snapshot.slowFrames)
        assertEquals(100.0, snapshot.worstFrameMs, 0.001)
        assertFalse(snapshot.meetsTarget)
    }

    @Test
    fun `idle time between camera moves is not a slow frame`() {
        val acc = MapFrameAccumulator()
        assertNull(acc.onFrame(0))
        assertNull(acc.onFrame(33))
        // Nothing changed for 900 ms (map idle), then motion resumes at 30 fps.
        val snapshot = acc.onFrame(933) ?: acc.onFrame(966) ?: acc.onFrame(1_000)
        assertNotNull(snapshot)
        assertEquals(0, snapshot!!.slowFrames)
        // Worst is the 966 -> 1000 ms frame (34 ms); the 900 ms idle gap is excluded.
        assertEquals(34.0, snapshot.worstFrameMs, 0.001)
    }

    @Test
    fun `diagnostics wording separates never-rendered, idle and measured`() {
        assertEquals("Not rendered yet — open Home or Maps", mapRenderReadingText(MapRenderSnapshot()))
        assertEquals("Idle (nothing moving)", mapRenderReadingText(MapRenderSnapshot(totalFrames = 10)))
        assertEquals(
            "30 fps · 0 slow · worst 34 ms · smooth",
            mapRenderReadingText(MapRenderSnapshot(activeFps = 30.2, worstFrameMs = 34.0, totalFrames = 99)),
        )
    }
}
