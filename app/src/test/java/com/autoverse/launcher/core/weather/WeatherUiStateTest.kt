package com.autoverse.launcher.core.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUiStateTest {

    @Test
    fun `available weather state exposes temperature and weather condition`() {
        val weather = WeatherUiState.Available(
            temperatureC = 22,
            conditionCode = 1,
            conditionText = "Mainly Clear",
            windSpeedKmh = 14,
            lastUpdatedFormatted = "14:30",
        )
        assertEquals(22, weather.temperatureC)
        assertEquals(1, weather.conditionCode)
        assertEquals("Mainly Clear", weather.conditionText)
        assertEquals(14, weather.windSpeedKmh)
        assertEquals("14:30", weather.lastUpdatedFormatted)
    }

    @Test
    fun `unavailable weather state contains clear explanation reason`() {
        val unavailable = WeatherUiState.Unavailable("Location not available")
        assertEquals("Location not available", unavailable.reason)
    }

    @Test
    fun `loading state is recognized as loading`() {
        val state: WeatherUiState = WeatherUiState.Loading
        assertTrue(state is WeatherUiState.Loading)
    }
}
