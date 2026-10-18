package com.autoverse.launcher.core.util

import java.time.LocalDateTime

/**
 * Milliseconds from [time] to the next wall-clock minute boundary, so a
 * minute-aligned ticker wakes exactly when the displayed minute changes
 * instead of running sixty times per minute. The result is clamped to
 * 1..60_000 ms: a value of 0 at an exact boundary would spin the coroutine.
 * Android-free for JVM tests (ClockTickerTest).
 */
fun millisUntilNextMinute(time: LocalDateTime): Long {
    val nanosIntoMinute = time.second * 1_000_000_000L + time.nano
    val remainingNanos = 60_000_000_000L - nanosIntoMinute
    return ((remainingNanos + 999_999L) / 1_000_000L).coerceIn(1L, 60_000L)
}
