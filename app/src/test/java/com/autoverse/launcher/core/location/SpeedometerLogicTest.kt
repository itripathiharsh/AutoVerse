package com.autoverse.launcher.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedometerLogicTest {

    @Test
    fun `fix without speed never produces a reading`() {
        assertNull(speedKmhFromFix(hasSpeed = false, speedMps = 27.78f, accuracyMeters = 5f))
    }

    @Test
    fun `metres per second converts to km per hour`() {
        assertEquals(100, speedKmhFromFix(hasSpeed = true, speedMps = 27.7778f, accuracyMeters = 5f))
        assertEquals(0, speedKmhFromFix(hasSpeed = true, speedMps = 0f, accuracyMeters = 5f))
    }

    @Test
    fun `inaccurate fixes are withheld instead of guessed`() {
        assertNull(
            speedKmhFromFix(
                hasSpeed = true,
                speedMps = 27.78f,
                accuracyMeters = SPEED_MAX_ACCURACY_M + 0.5f,
            ),
        )
    }

    @Test
    fun `unknown accuracy still yields a reading`() {
        assertEquals(36, speedKmhFromFix(hasSpeed = true, speedMps = 10f, accuracyMeters = null))
    }

    @Test
    fun `negative speeds clamp to zero`() {
        assertEquals(0, speedKmhFromFix(hasSpeed = true, speedMps = -3f, accuracyMeters = 5f))
    }

    @Test
    fun `stale threshold sits at five seconds`() {
        assertFalse(isFixStale(fixElapsedMs = 0L, nowElapsedMs = SPEED_STALE_AFTER_MS))
        assertTrue(isFixStale(fixElapsedMs = 0L, nowElapsedMs = SPEED_STALE_AFTER_MS + 1))
        assertFalse(isFixStale(fixElapsedMs = 10_000L, nowElapsedMs = 12_000L))
    }

    @Test
    fun `every gps status the panel words is present`() {
        assertEquals(
            listOf(
                GpsStatus.NO_ACCESS,
                GpsStatus.PROVIDER_OFF,
                GpsStatus.WAITING,
                GpsStatus.LIVE,
                GpsStatus.STALE,
            ),
            GpsStatus.entries.toList(),
        )
    }

    @Test
    fun `default reading state is waiting with no speed`() {
        val data = SpeedometerData()
        assertEquals(GpsStatus.WAITING, data.gpsStatus)
        assertNull(data.currentSpeedKmh)
    }
}
