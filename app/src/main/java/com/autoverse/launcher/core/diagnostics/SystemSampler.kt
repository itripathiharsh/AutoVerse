package com.autoverse.launcher.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.StatFs
import java.io.File

/**
 * One reading of process and device resource usage. Every field is nullable:
 * null means "not readable right now" and the UI must show that instead of a
 * placeholder number.
 *
 * Units are deliberate — [processCpuPercentOfCore] is the process's share of
 * ONE CPU core over the sampling interval (100% = one full core, values above
 * 100 are possible on multi-threaded peaks), and the memory fields are
 * process PSS vs whole-device memory, never mixed.
 */
data class SystemSnapshot(
    val processCpuPercentOfCore: Float? = null,
    val processMemoryPssMb: Int? = null,
    val deviceMemoryAvailableMb: Int? = null,
    val deviceMemoryTotalMb: Int? = null,
    val storageAvailableMb: Int? = null,
)

/**
 * Phase 17 — reads real resource usage for the diagnostics performance panel.
 *
 * CPU is derived from `/proc/self/stat` (utime + stime ticks) divided by the
 * elapsed interval and the kernel clock tick rate — this is the process's own
 * CPU time, so it is a defensible, device-independent measurement. The first
 * sample returns null (a delta needs two readings). Memory uses
 * [Debug.MemoryInfo] total PSS for the process and [ActivityManager] memory
 * info for the device; storage uses [StatFs] on the app's cache filesystem.
 *
 * Every read is individually guarded: a denied or absent source yields null
 * rather than a fabricated value. [sample] is synchronous and must be called
 * off the main thread.
 */
class SystemSampler(
    private val context: Context,
    private val nanoTime: () -> Long = System::nanoTime,
) {

    private val clockTicksPerSecond: Long = try {
        android.system.Os.sysconf(android.system.OsConstants._SC_CLK_TCK)
    } catch (_: Exception) {
        FALLBACK_CLOCK_TICKS
    }

    private var previousCpuTicks: Long? = null
    private var previousNanoTime: Long? = null

    @Synchronized
    fun sample(): SystemSnapshot {
        val cpuTicks = try {
            parseCpuStatTotalTicks(File(PROC_SELF_STAT).readText())
        } catch (_: Exception) {
            null
        }
        val nowNanos = nanoTime()
        val cpuPercent = if (cpuTicks != null) {
            val percent = cpuPercentOfCore(
                previousTicks = previousCpuTicks,
                previousNanos = previousNanoTime,
                ticks = cpuTicks,
                nanos = nowNanos,
                clockTicksPerSecond = clockTicksPerSecond,
            )
            previousCpuTicks = cpuTicks
            previousNanoTime = nowNanos
            percent
        } else {
            null
        }
        return SystemSnapshot(
            processCpuPercentOfCore = cpuPercent,
            processMemoryPssMb = readProcessMemoryPssMb(),
            deviceMemoryAvailableMb = readDeviceMemoryMb(available = true),
            deviceMemoryTotalMb = readDeviceMemoryMb(available = false),
            storageAvailableMb = readStorageAvailableMb(),
        )
    }

    private fun readProcessMemoryPssMb(): Int? = try {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        (info.totalPss / 1024).takeIf { it > 0 }
    } catch (_: Exception) {
        null
    }

    private fun readDeviceMemoryMb(available: Boolean): Int? = try {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        if (manager == null) {
            null
        } else {
            val info = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(info)
            val bytes = if (available) info.availMem else info.totalMem
            (bytes / BYTES_PER_MB).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }
    } catch (_: Exception) {
        null
    }

    private fun readStorageAvailableMb(): Int? = try {
        val stats = StatFs(context.filesDir.path)
        (stats.availableBytes / BYTES_PER_MB).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val PROC_SELF_STAT = "/proc/self/stat"
        const val BYTES_PER_MB = 1024L * 1024L
        const val FALLBACK_CLOCK_TICKS = 100L
    }
}

/**
 * Sums utime + stime from one `/proc/<pid>/stat` line. The comm field is
 * wrapped in parentheses and may itself contain spaces or parentheses, so
 * parsing starts after the LAST ')' — the following space-separated fields
 * then start at state, making utime index 11 and stime index 12. Returns null
 * for malformed lines so callers can degrade to "unavailable".
 */
internal fun parseCpuStatTotalTicks(statLine: String): Long? {
    val commEnd = statLine.lastIndexOf(')')
    if (commEnd < 0) return null
    val fields = statLine.substring(commEnd + 1).trim().split(WHITESPACE)
    if (fields.size < 13) return null
    val userTicks = fields[11].toLongOrNull() ?: return null
    val systemTicks = fields[12].toLongOrNull() ?: return null
    return userTicks + systemTicks
}

/**
 * Process CPU as a percentage of one core over the interval between two
 * readings. Needs a previous reading, a positive elapsed interval, and a
 * known tick rate; anything else returns null. Pure so the math is
 * unit-tested without Android.
 */
internal fun cpuPercentOfCore(
    previousTicks: Long?,
    previousNanos: Long?,
    ticks: Long,
    nanos: Long,
    clockTicksPerSecond: Long,
): Float? {
    if (previousTicks == null || previousNanos == null) return null
    if (clockTicksPerSecond <= 0L) return null
    val deltaTicks = ticks - previousTicks
    val deltaNanos = nanos - previousNanos
    if (deltaTicks < 0L || deltaNanos <= 0L) return null
    val elapsedSeconds = deltaNanos / 1_000_000_000.0
    return (deltaTicks.toDouble() / clockTicksPerSecond / elapsedSeconds * 100.0).toFloat()
}

private val WHITESPACE = Regex("\\s+")
