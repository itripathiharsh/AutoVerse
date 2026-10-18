package com.autoverse.launcher.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatClock(
    time: LocalTime,
    clock24h: Boolean,
    locale: Locale = Locale.getDefault(),
): String {
    val pattern = if (clock24h) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern, locale).format(time)
}

fun formatFullDate(
    date: LocalDate,
    locale: Locale = Locale.getDefault(),
): String = DateTimeFormatter.ofPattern("EEE, d MMM", locale).format(date)

/**
 * Timestamp for diagnostics log rows: wall-clock "HH:mm:ss" in the given
 * zone. Digits only, so no locale is needed; the default zone keeps the rows
 * aligned with the dashboard clock.
 */
fun formatLogTimestamp(
    timeMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = LOG_TIME_FORMATTER.format(Instant.ofEpochMilli(timeMillis).atZone(zoneId))

private val LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * Track position / duration for the media surface: "m:ss" under an hour and
 * "h:mm:ss" from an hour up. Negative input clamps to zero.
 */
fun formatTrackTime(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val paddedSeconds = seconds.toString().padStart(2, '0')
    return if (hours > 0L) {
        val paddedMinutes = minutes.toString().padStart(2, '0')
        "$hours:$paddedMinutes:$paddedSeconds"
    } else {
        "$minutes:$paddedSeconds"
    }
}
