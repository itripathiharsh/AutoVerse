package com.autoverse.launcher.core.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedometerDataTest {

    @Test
    fun `default speedometer data starts with null speed and inactive gps`() {
        val data = SpeedometerData()
        assertNull(data.currentSpeedKmh)
        assertEquals(0, data.maxSpeedKmh)
        assertEquals(0.0, data.tripDistanceMeters, 0.001)
        assertEquals(0.0, data.tripDistanceKm, 0.001)
        assertEquals(0L, data.tripDurationSeconds)
        assertFalse(data.isGpsActive)
        assertEquals("0.0 km", data.tripDistanceFormatted)
        assertEquals("00:00", data.tripDurationFormatted)
    }

    @Test
    fun `speedometer data correctly calculates trip distance and duration formatted`() {
        val data = SpeedometerData(
            currentSpeedKmh = 72,
            maxSpeedKmh = 110,
            tripDistanceMeters = 14500.0, // 14.5 km
            tripDurationSeconds = 3665L, // 1h 1m 5s
            isGpsActive = true,
            accuracyMeters = 3.5f,
            latitude = 37.7749,
            longitude = -122.4194,
        )
        assertEquals(72, data.currentSpeedKmh)
        assertEquals(110, data.maxSpeedKmh)
        assertEquals(14.5, data.tripDistanceKm, 0.001)
        assertEquals("14.5 km", data.tripDistanceFormatted)
        assertEquals("01:01:05", data.tripDurationFormatted)
        assertTrue(data.isGpsActive)
        assertEquals(3.5f, data.accuracyMeters ?: 0f, 0.01f)
        assertEquals(37.7749, data.latitude ?: 0.0, 0.0001)
        assertEquals(-122.4194, data.longitude ?: 0.0, 0.0001)
    }
}
