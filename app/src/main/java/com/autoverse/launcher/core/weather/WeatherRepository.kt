package com.autoverse.launcher.core.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Unavailable(val reason: String) : WeatherUiState
    data class Available(
        val temperatureC: Int,
        val conditionCode: Int,
        val conditionText: String,
        val windSpeedKmh: Int,
        val lastUpdatedFormatted: String,
    ) : WeatherUiState
}

class WeatherRepository {

    private val _weatherState = MutableStateFlow<WeatherUiState>(WeatherUiState.Unavailable("Awaiting location"))
    val weatherState: StateFlow<WeatherUiState> = _weatherState.asStateFlow()

    private var lastFetchedLat: Double? = null
    private var lastFetchedLng: Double? = null
    private var lastFetchedTimestampMs: Long = 0L

    /**
     * Fetches current weather from Open-Meteo (open-source, zero credentials).
     * Caches responses for 20 minutes unless location changes by > 0.08 degrees (~8km).
     */
    suspend fun updateWeather(latitude: Double?, longitude: Double?, force: Boolean = false) {
        if (latitude == null || longitude == null) {
            if (_weatherState.value !is WeatherUiState.Available) {
                _weatherState.value = WeatherUiState.Unavailable("GPS location not available")
            }
            return
        }

        val nowMs = System.currentTimeMillis()
        val cacheValid = !force &&
            (nowMs - lastFetchedTimestampMs < 20 * 60 * 1000L) &&
            lastFetchedLat != null &&
            lastFetchedLng != null &&
            abs(latitude - lastFetchedLat!!) < 0.08 &&
            abs(longitude - lastFetchedLng!!) < 0.08

        if (cacheValid) return

        withContext(Dispatchers.IO) {
            if (_weatherState.value !is WeatherUiState.Available) {
                _weatherState.value = WeatherUiState.Loading
            }
            var connection: HttpURLConnection? = null
            try {
                val urlString = "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code,wind_speed_10m"
                    .format(Locale.US, latitude, longitude)
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    requestMethod = "GET"
                    setRequestProperty("User-Agent", "AutoVerse-Automotive-Launcher/1.0")
                }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val body = reader.readText()
                    reader.close()

                    val json = JSONObject(body)
                    val current = json.getJSONObject("current")
                    val temp = current.getDouble("temperature_2m").roundToInt()
                    val code = current.getInt("weather_code")
                    val wind = current.getDouble("wind_speed_10m").roundToInt()

                    val condition = weatherCodeToText(code)
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val updatedStr = timeFormat.format(Date())

                    lastFetchedLat = latitude
                    lastFetchedLng = longitude
                    lastFetchedTimestampMs = nowMs

                    _weatherState.value = WeatherUiState.Available(
                        temperatureC = temp,
                        conditionCode = code,
                        conditionText = condition,
                        windSpeedKmh = wind,
                        lastUpdatedFormatted = updatedStr,
                    )
                } else {
                    _weatherState.value = WeatherUiState.Unavailable("Weather service temporarily unavailable")
                }
            } catch (_: Exception) {
                // If offline or timeout, retain last known if available, else show offline
                if (_weatherState.value !is WeatherUiState.Available) {
                    _weatherState.value = WeatherUiState.Unavailable("Offline \u2014 network unavailable")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    companion object {
        fun weatherCodeToText(code: Int): String = when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rain"
            71, 73, 75 -> "Snow"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Fair"
        }
    }
}
