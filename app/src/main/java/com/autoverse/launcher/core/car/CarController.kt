package com.autoverse.launcher.core.car

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoverse.launcher.core.location.GpsStatus
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.routing.GeoPoint
import com.autoverse.launcher.core.routing.RoutingDataStore
import com.autoverse.launcher.core.routing.distanceMeters
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * The Car page's state: automatic trip log, fuel cost, reminders (service /
 * PUC / insurance), the speaker test and the head-unit health check. All
 * data stays on the unit (SharedPreferences).
 */
class CarController(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("autoverse_car", Context.MODE_PRIVATE)
    private val health = HeadUnitHealth(app)
    private val roadData = RoutingDataStore(app)

    private val _trips = MutableStateFlow(loadTrips())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _fuel = MutableStateFlow(
        FuelSettings(
            pricePerLitre = prefs.getFloat(KEY_FUEL_PRICE, 105f).toDouble(),
            kmPerLitre = prefs.getFloat(KEY_MILEAGE, 15f).toDouble(),
        ),
    )
    val fuel: StateFlow<FuelSettings> = _fuel.asStateFlow()

    private val _reminders = MutableStateFlow(loadReminders())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    /** Odometer the driver entered + km AutoVerse logged since. Null until entered. */
    private val _odometer = MutableStateFlow(currentOdometer())
    val odometerKm: StateFlow<Int?> = _odometer.asStateFlow()

    private val _health = MutableStateFlow<HealthReport?>(null)
    val healthReport: StateFlow<HealthReport?> = _health.asStateFlow()

    private val _playing = MutableStateFlow<SpeakerTone?>(null)
    val playingTone: StateFlow<SpeakerTone?> = _playing.asStateFlow()
    private var toneJob: Job? = null

    // ---- automatic trip log -------------------------------------------------

    private var tripStartMs = 0L
    private var tripKm = 0.0
    private var tripMax = 0
    private var lastPoint: GeoPoint? = null
    private var lastMovingMs = 0L

    fun onLocation(d: SpeedometerData) {
        if (d.gpsStatus != GpsStatus.LIVE) return
        val lat = d.latitude ?: return
        val lon = d.longitude ?: return
        val now = System.currentTimeMillis()
        val speed = d.currentSpeedKmh ?: 0
        val p = GeoPoint(lat, lon)
        if (tripStartMs == 0L) {
            if (speed >= TRIP_START_KMH) {
                tripStartMs = now; tripKm = 0.0; tripMax = speed; lastPoint = p; lastMovingMs = now
            }
            return
        }
        lastPoint?.let { prev ->
            val m = distanceMeters(prev, p)
            if (m in 2.0..500.0) {
                tripKm += m / 1000.0
                addLoggedKm(m / 1000.0)
            }
        }
        lastPoint = p
        if (speed > tripMax) tripMax = speed
        if (speed >= 3) lastMovingMs = now
        if (now - lastMovingMs > TRIP_END_IDLE_MS) finishTrip(lastMovingMs)
    }

    /** Called when AutoVerse goes to the background for long, or the page asks to close the trip. */
    fun finishTrip(endMs: Long = System.currentTimeMillis()) {
        if (tripStartMs != 0L && tripKm >= TRIP_MIN_KM) {
            val trip = Trip(tripStartMs, endMs, tripKm, tripMax)
            _trips.value = (listOf(trip) + _trips.value).take(MAX_TRIPS)
            saveTrips()
        }
        tripStartMs = 0L; tripKm = 0.0; tripMax = 0; lastPoint = null
    }

    val tripInProgressKm: Double get() = if (tripStartMs != 0L) tripKm else 0.0

    fun clearTrips() {
        _trips.value = emptyList()
        saveTrips()
    }

    private var unsavedKm = 0.0
    private fun addLoggedKm(km: Double) {
        unsavedKm += km
        if (unsavedKm >= 0.5) {
            prefs.edit().putFloat(KEY_LOGGED_KM, prefs.getFloat(KEY_LOGGED_KM, 0f) + unsavedKm.toFloat()).apply()
            unsavedKm = 0.0
            _odometer.value = currentOdometer()
        }
    }

    // ---- fuel ---------------------------------------------------------------

    fun setFuel(pricePerLitre: Double, kmPerLitre: Double) {
        val f = FuelSettings(pricePerLitre.coerceIn(1.0, 1000.0), kmPerLitre.coerceIn(1.0, 100.0))
        _fuel.value = f
        prefs.edit().putFloat(KEY_FUEL_PRICE, f.pricePerLitre.toFloat()).putFloat(KEY_MILEAGE, f.kmPerLitre.toFloat()).apply()
    }

    // ---- reminders ----------------------------------------------------------

    fun setReminder(reminder: Reminder) {
        _reminders.value = _reminders.value.filterNot { it.kind == reminder.kind } + reminder
        saveReminders()
    }

    fun clearReminder(kind: ReminderKind) {
        _reminders.value = _reminders.value.filterNot { it.kind == kind }
        saveReminders()
    }

    /** The driver reads the odometer once; AutoVerse adds its own logged km after that. */
    fun setOdometer(km: Int) {
        prefs.edit()
            .putInt(KEY_ODO_BASE, km.coerceAtLeast(0))
            .putFloat(KEY_ODO_LOGGED_AT_BASE, prefs.getFloat(KEY_LOGGED_KM, 0f))
            .apply()
        _odometer.value = currentOdometer()
    }

    private fun currentOdometer(): Int? {
        if (!prefs.contains(KEY_ODO_BASE)) return null
        val base = prefs.getInt(KEY_ODO_BASE, 0)
        val since = prefs.getFloat(KEY_LOGGED_KM, 0f) - prefs.getFloat(KEY_ODO_LOGGED_AT_BASE, 0f)
        return base + since.toInt()
    }

    /** Reminders that need attention now, most urgent first (for the Home banner). */
    fun urgentReminders(today: LocalDate = LocalDate.now()): List<Pair<Reminder, ReminderUrgency>> =
        _reminders.value
            .map { it to reminderUrgency(it, today, _odometer.value) }
            .filter { it.second != ReminderUrgency.OK }
            .sortedByDescending { it.second }

    // ---- speaker test -------------------------------------------------------

    fun playTone(tone: SpeakerTone) {
        toneJob?.cancel()
        toneJob = viewModelScope.launch {
            _playing.value = tone
            try {
                SpeakerTest.play(tone)
            } catch (_: Exception) {
            } finally {
                if (_playing.value == tone) _playing.value = null
            }
        }
    }

    fun stopTone() {
        toneJob?.cancel()
        _playing.value = null
    }

    // ---- head unit health ---------------------------------------------------

    fun refreshHealth() {
        viewModelScope.launch {
            val roadBytes = roadData.segmentDir.listFiles()?.sumOf { it.length() } ?: 0L
            _health.value = health.report(roadBytes)
        }
    }

    fun clearRoadData() {
        roadData.clear()
        refreshHealth()
    }

    // ---- storage --------------------------------------------------------------

    private fun loadTrips(): List<Trip> = try {
        val arr = JSONArray(prefs.getString(KEY_TRIPS, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Trip(o.getLong("s"), o.getLong("e"), o.getDouble("km"), o.getInt("max"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveTrips() {
        val arr = JSONArray()
        _trips.value.forEach { arr.put(JSONObject().put("s", it.startMs).put("e", it.endMs).put("km", it.distanceKm).put("max", it.maxSpeedKmh)) }
        prefs.edit().putString(KEY_TRIPS, arr.toString()).apply()
    }

    private fun loadReminders(): List<Reminder> = try {
        val arr = JSONArray(prefs.getString(KEY_REMINDERS, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            Reminder(
                kind = ReminderKind.valueOf(o.getString("k")),
                dueDate = o.optString("d").takeIf { it.isNotBlank() }?.let(LocalDate::parse),
                dueOdometerKm = if (o.has("km")) o.getInt("km") else null,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveReminders() {
        val arr = JSONArray()
        _reminders.value.forEach { r ->
            val o = JSONObject().put("k", r.kind.name)
            r.dueDate?.let { o.put("d", it.toString()) }
            r.dueOdometerKm?.let { o.put("km", it) }
            arr.put(o)
        }
        prefs.edit().putString(KEY_REMINDERS, arr.toString()).apply()
    }

    override fun onCleared() {
        finishTrip()
        super.onCleared()
    }

    private companion object {
        const val MAX_TRIPS = 100
        const val KEY_TRIPS = "trips"
        const val KEY_REMINDERS = "reminders"
        const val KEY_FUEL_PRICE = "fuel_price"
        const val KEY_MILEAGE = "mileage"
        const val KEY_LOGGED_KM = "logged_km"
        const val KEY_ODO_BASE = "odo_base"
        const val KEY_ODO_LOGGED_AT_BASE = "odo_logged_at_base"
    }
}
