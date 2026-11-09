package com.autoverse.launcher.core.car

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/** One drive, recorded automatically from GPS. */
data class Trip(
    val startMs: Long,
    val endMs: Long,
    val distanceKm: Double,
    val maxSpeedKmh: Int,
) {
    val durationMin: Int get() = ((endMs - startMs) / 60_000L).toInt().coerceAtLeast(1)
    val avgSpeedKmh: Int get() = (distanceKm / (durationMin / 60.0)).roundToInt()
}

/** Fuel price (₹ per litre) and the car's mileage (km per litre). */
data class FuelSettings(val pricePerLitre: Double = 105.0, val kmPerLitre: Double = 15.0) {
    fun costFor(distanceKm: Double): Double = if (kmPerLitre <= 0) 0.0 else distanceKm / kmPerLitre * pricePerLitre
    fun litresFor(distanceKm: Double): Double = if (kmPerLitre <= 0) 0.0 else distanceKm / kmPerLitre
}

/** Things a car owner in India must not miss. */
enum class ReminderKind(val title: String) {
    SERVICE("Car service"),
    PUC("PUC certificate"),
    INSURANCE("Insurance"),
}

/**
 * A reminder by date, and for service optionally by distance: due at
 * [dueOdometerKm] measured against the odometer the driver entered plus the
 * kilometres AutoVerse has logged since.
 */
data class Reminder(
    val kind: ReminderKind,
    val dueDate: LocalDate? = null,
    val dueOdometerKm: Int? = null,
)

enum class ReminderUrgency { OK, SOON, DUE }

/** Days within which a date reminder is "soon"; kilometres for service. */
const val REMINDER_SOON_DAYS = 15
const val REMINDER_SOON_KM = 500

fun reminderUrgency(reminder: Reminder, today: LocalDate, odometerKm: Int?): ReminderUrgency {
    var urgency = ReminderUrgency.OK
    reminder.dueDate?.let { due ->
        val days = ChronoUnit.DAYS.between(today, due)
        urgency = maxOf(urgency, if (days < 0) ReminderUrgency.DUE else if (days <= REMINDER_SOON_DAYS) ReminderUrgency.SOON else ReminderUrgency.OK)
    }
    if (reminder.dueOdometerKm != null && odometerKm != null) {
        val left = reminder.dueOdometerKm - odometerKm
        urgency = maxOf(urgency, if (left <= 0) ReminderUrgency.DUE else if (left <= REMINDER_SOON_KM) ReminderUrgency.SOON else ReminderUrgency.OK)
    }
    return urgency
}

/** "in 12 days", "today", "3 days ago"; "in 420 km". */
fun reminderDetail(reminder: Reminder, today: LocalDate, odometerKm: Int?): String {
    val parts = mutableListOf<String>()
    reminder.dueDate?.let { due ->
        val days = ChronoUnit.DAYS.between(today, due)
        parts += when {
            days == 0L -> "due today"
            days < 0 -> "overdue by ${-days} day${if (days == -1L) "" else "s"}"
            else -> "in $days day${if (days == 1L) "" else "s"}"
        }
    }
    if (reminder.dueOdometerKm != null && odometerKm != null) {
        val left = reminder.dueOdometerKm - odometerKm
        parts += if (left <= 0) "overdue by ${-left} km" else "in $left km"
    }
    return parts.joinToString(" · ").ifEmpty { "not set" }
}

/**
 * Trip detection: a trip starts once the car moves faster than
 * [TRIP_START_KMH] and ends after [TRIP_END_IDLE_MS] without movement. Trips
 * shorter than [TRIP_MIN_KM] (parking, GPS drift) are discarded.
 */
const val TRIP_START_KMH = 8
const val TRIP_END_IDLE_MS = 5 * 60_000L
const val TRIP_MIN_KM = 0.3

/** How capable this head unit is, from RAM — said plainly. */
enum class UnitClass(val label: String, val advice: String) {
    ENTRY(
        "Entry-level unit",
        "Under 3 GB of RAM: AutoVerse runs in its lightest mode. Keep only the apps you use, and avoid running other navigation apps in the background.",
    ),
    MID(
        "Mid-range unit",
        "3–5 GB of RAM: everything in AutoVerse runs comfortably. Removing unused pre-installed apps keeps it quick.",
    ),
    HIGH(
        "High-end unit",
        "6 GB of RAM or more: plenty of headroom for maps, music and other apps together.",
    ),
}

fun unitClassFor(totalRamMb: Long): UnitClass = when {
    totalRamMb < 3_000 -> UnitClass.ENTRY
    totalRamMb < 5_500 -> UnitClass.MID
    else -> UnitClass.HIGH
}

/** Storage below this share free makes Android slow (it starts trimming caches constantly). */
const val LOW_STORAGE_FRACTION = 0.10

/** Map tile cache size for the free space: 200 MB at most, never more than 5% of free, at least 50 MB. */
fun mapCacheBytesFor(freeBytes: Long): Long {
    val max = 200L * 1024 * 1024
    val min = 50L * 1024 * 1024
    return (freeBytes / 20).coerceIn(min, max)
}
