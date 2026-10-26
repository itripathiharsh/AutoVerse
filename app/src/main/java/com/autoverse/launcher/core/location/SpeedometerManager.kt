package com.autoverse.launcher.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

/** Where the speed reading stands right now; the UI words each state honestly. */
enum class GpsStatus {
    /** Location permission is not granted. */
    NO_ACCESS,

    /** No location provider is enabled (GPS switched off). */
    PROVIDER_OFF,

    /** Listening, but no fresh fix has arrived yet. */
    WAITING,

    /** A fix arrived within [SPEED_STALE_AFTER_MS]. */
    LIVE,

    /** Fixes stopped arriving (tunnel, garage); the last speed is withheld. */
    STALE,
}

data class SpeedometerData(
    val currentSpeedKmh: Int? = null,
    val isGpsActive: Boolean = false,
    val accuracyMeters: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tripDistanceMeters: Double = 0.0,
    val tripDurationSeconds: Long = 0L,
    val maxSpeedKmh: Int = 0,
    val gpsStatus: GpsStatus = GpsStatus.WAITING,
    /** Course over ground from the fix (degrees), when the receiver reports one. */
    val bearingDegrees: Float? = null,
    /** Elapsed-realtime of the fix, so consumers can tell a new fix from a recomposition. */
    val fixElapsedMs: Long = 0L,
) {
    val tripDistanceKm: Double
        get() = tripDistanceMeters / 1000.0

    val tripDistanceFormatted: String
        get() = "%.1f km".format(tripDistanceKm)

    val tripDurationFormatted: String
        get() {
            val hours = tripDurationSeconds / 3600
            val minutes = (tripDurationSeconds % 3600) / 60
            val seconds = tripDurationSeconds % 60
            return if (hours > 0) {
                "%02d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
}

/** No fix for this long and the reading is withheld instead of frozen on screen. */
const val SPEED_STALE_AFTER_MS = 5_000L

/** GPS update interval: once a second is plenty for a speed readout. */
const val SPEED_UPDATE_INTERVAL_MS = 1_000L

/** Fixes reporting worse accuracy than this never set the speed. */
const val SPEED_MAX_ACCURACY_M = 60f

/**
 * km/h from one fix, or null when the fix carries no speed or is too
 * inaccurate to trust. Pure for JVM tests.
 */
fun speedKmhFromFix(hasSpeed: Boolean, speedMps: Float, accuracyMeters: Float?): Int? {
    if (!hasSpeed) return null
    if (accuracyMeters != null && accuracyMeters > SPEED_MAX_ACCURACY_M) return null
    return (speedMps * 3.6f).roundToInt().coerceAtLeast(0)
}

/** True when a fix taken at [fixElapsedMs] is too old to show as current speed. */
fun isFixStale(fixElapsedMs: Long, nowElapsedMs: Long): Boolean =
    nowElapsedMs - fixElapsedMs > SPEED_STALE_AFTER_MS

/**
 * GPS speed and trip tracking over LocationManager. Updates are pushed by
 * the platform once a second while started; a single delayed check marks the
 * reading stale when fixes stop, so a tunnel never leaves a frozen number on
 * screen labelled "live". Cached last-known locations seed the position (for
 * weather) but never the speed unless they are fresh.
 */
class SpeedometerManager(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _speedometerData = MutableStateFlow(SpeedometerData())
    val speedometerData: StateFlow<SpeedometerData> = _speedometerData.asStateFlow()

    private var isListening = false
    private var lastLocation: Location? = null
    private var tripStartTimeMs: Long? = null
    private var accumulatedTripSeconds: Long = 0L
    private var tripDistanceMeters: Double = 0.0
    private var maxSpeedKmh: Int = 0

    val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

    private val markStale = Runnable {
        val current = _speedometerData.value
        if (current.gpsStatus == GpsStatus.LIVE) {
            _speedometerData.value = current.copy(
                currentSpeedKmh = null,
                isGpsActive = false,
                gpsStatus = GpsStatus.STALE,
            )
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) = applyFix(location, isLive = true)

        override fun onProviderEnabled(provider: String) {
            if (_speedometerData.value.gpsStatus == GpsStatus.PROVIDER_OFF) {
                setStatus(GpsStatus.WAITING)
            }
        }

        override fun onProviderDisabled(provider: String) {
            if (!anyProviderEnabled()) {
                mainHandler.removeCallbacks(markStale)
                setStatus(GpsStatus.PROVIDER_OFF)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    private fun applyFix(location: Location, isLive: Boolean) {
        val nowMs = SystemClock.elapsedRealtime()
        val accuracy = if (location.hasAccuracy()) location.accuracy else null
        val trusted = accuracy == null || accuracy <= SPEED_MAX_ACCURACY_M
        val speedKmh = if (isLive) speedKmhFromFix(location.hasSpeed(), location.speed, accuracy) else null

        if (isLive) {
            if (tripStartTimeMs == null) tripStartTimeMs = nowMs
            if (speedKmh != null && speedKmh > maxSpeedKmh) maxSpeedKmh = speedKmh
            val prev = lastLocation
            if (prev != null && trusted) {
                val dist = prev.distanceTo(location)
                // Ignore stationary GPS jitter.
                if (dist > 1.5f && (speedKmh == null || speedKmh > 0)) tripDistanceMeters += dist
            }
            lastLocation = location
            mainHandler.removeCallbacks(markStale)
            mainHandler.postDelayed(markStale, SPEED_STALE_AFTER_MS)
        }

        val tripSeconds = tripStartTimeMs?.let { accumulatedTripSeconds + (nowMs - it) / 1000L }
            ?: accumulatedTripSeconds
        _speedometerData.value = SpeedometerData(
            currentSpeedKmh = speedKmh,
            isGpsActive = isLive,
            accuracyMeters = accuracy,
            latitude = location.latitude,
            longitude = location.longitude,
            tripDistanceMeters = tripDistanceMeters,
            tripDurationSeconds = tripSeconds,
            maxSpeedKmh = maxSpeedKmh,
            gpsStatus = if (isLive) GpsStatus.LIVE else _speedometerData.value.gpsStatus,
            bearingDegrees = if (location.hasBearing()) location.bearing else null,
            fixElapsedMs = location.elapsedRealtimeNanos / 1_000_000L,
        )
    }

    private fun setStatus(status: GpsStatus) {
        _speedometerData.value = _speedometerData.value.copy(
            gpsStatus = status,
            isGpsActive = status == GpsStatus.LIVE,
            currentSpeedKmh = if (status == GpsStatus.LIVE) _speedometerData.value.currentSpeedKmh else null,
        )
    }

    private fun anyProviderEnabled(): Boolean = try {
        locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    } catch (_: Exception) {
        false
    }

    /** Safe to call repeatedly; call again after a permission grant. */
    @SuppressLint("MissingPermission")
    fun start() {
        if (isListening || locationManager == null) return
        if (!hasLocationPermission) {
            setStatus(GpsStatus.NO_ACCESS)
            return
        }
        try {
            val providers = locationManager.allProviders
            // Register even when switched off: onProviderEnabled then resumes
            // the readout without the user having to reopen AutoVerse.
            val provider = when {
                LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
                LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider == null) {
                setStatus(GpsStatus.PROVIDER_OFF)
                return
            }
            locationManager.requestLocationUpdates(
                provider,
                SPEED_UPDATE_INTERVAL_MS,
                0f,
                locationListener,
                Looper.getMainLooper(),
            )
            isListening = true
            setStatus(if (anyProviderEnabled()) GpsStatus.WAITING else GpsStatus.PROVIDER_OFF)

            // A cached fix seeds the position (weather) immediately; it only
            // counts as a live speed when it is genuinely fresh.
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastKnown != null) {
                val fixElapsedMs = lastKnown.elapsedRealtimeNanos / 1_000_000L
                applyFix(lastKnown, isLive = !isFixStale(fixElapsedMs, SystemClock.elapsedRealtime()))
            }
        } catch (_: SecurityException) {
            setStatus(GpsStatus.NO_ACCESS)
        } catch (_: IllegalArgumentException) {
            setStatus(GpsStatus.PROVIDER_OFF)
        }
    }

    fun stop() {
        mainHandler.removeCallbacks(markStale)
        if (!isListening || locationManager == null) return
        try {
            locationManager.removeUpdates(locationListener)
        } catch (_: SecurityException) {}
        isListening = false
        setStatus(GpsStatus.WAITING)

        tripStartTimeMs?.let {
            accumulatedTripSeconds += (SystemClock.elapsedRealtime() - it) / 1000L
            tripStartTimeMs = null
        }
    }

    fun resetTrip() {
        tripDistanceMeters = 0.0
        accumulatedTripSeconds = 0L
        tripStartTimeMs = if (isListening) SystemClock.elapsedRealtime() else null
        maxSpeedKmh = 0
        _speedometerData.value = _speedometerData.value.copy(
            tripDistanceMeters = 0.0,
            tripDurationSeconds = 0L,
            maxSpeedKmh = 0,
        )
    }
}
