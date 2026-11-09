package com.autoverse.launcher.core.car

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** One app in the health report. */
data class AppUse(
    val packageName: String,
    val label: String,
    val foregroundMinutes24h: Long,
    val lastUsedDaysAgo: Int?,
    val preinstalled: Boolean,
)

/** An honest snapshot of the head unit — no "boost" tricks, just facts and advice. */
data class HealthReport(
    val model: String,
    val androidVersion: String,
    val cpuCores: Int,
    val cpuMaxMhz: Int?,
    val totalRamMb: Long,
    val availRamMb: Long,
    val lowMemory: Boolean,
    val storageTotalGb: Double,
    val storageFreeGb: Double,
    val thermal: String?,
    val uptimeHours: Long,
    val unitClass: UnitClass,
    val usageAccess: Boolean,
    /** Most-used apps in the last 24 h (needs usage access). */
    val busiestApps: List<AppUse>,
    /** Apps not opened in 30+ days — candidates to disable or uninstall. */
    val unusedApps: List<AppUse>,
    val autoverseRoadDataMb: Long,
    val autoverseMapCacheMb: Long,
) {
    val storageLow: Boolean get() = storageTotalGb > 0 && storageFreeGb / storageTotalGb < LOW_STORAGE_FRACTION
    val ramUsedPercent: Int get() = if (totalRamMb > 0) (100 - availRamMb * 100 / totalRamMb).toInt() else 0

    /** Plain-language suggestions, most important first. */
    val tips: List<String>
        get() = buildList {
            if (storageLow) add("Storage is almost full — Android slows down below 10% free. Remove unused apps or old files.")
            if (lowMemory || ramUsedPercent > 85) add("Memory is nearly full right now. Close navigation or video apps you aren't using.")
            if (uptimeHours > 72) add("The unit hasn't restarted in ${uptimeHours / 24} days. A restart clears built-up memory.")
            if (unusedApps.isNotEmpty()) add("${unusedApps.size} apps haven't been opened in 30 days. Disabling them frees memory and startup time.")
            if (thermal != null && thermal != "Normal") add("The unit is running $thermal — it will slow itself down to cool off. Avoid direct sun on the screen.")
            if (isEmpty()) add("Everything looks healthy.")
        }
}

/**
 * Builds the head-unit health report. Reads only what Android exposes to a
 * normal app; per-app usage needs the user to grant Usage access (optional).
 * Nothing here kills or "boosts" anything — Android doesn't allow it, and
 * apps that pretend to do it make units slower.
 */
class HeadUnitHealth(context: Context) {
    private val app = context.applicationContext

    fun hasUsageAccess(): Boolean = try {
        val ops = app.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    suspend fun report(roadDataBytes: Long): HealthReport = withContext(Dispatchers.IO) {
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().path)
        val totalRamMb = mem.totalMem / (1024 * 1024)
        val usage = hasUsageAccess()
        val (busiest, unused) = if (usage) appUsage() else emptyList<AppUse>() to emptyList()
        HealthReport(
            model = listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" "),
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            cpuCores = Runtime.getRuntime().availableProcessors(),
            cpuMaxMhz = readCpuMaxMhz(),
            totalRamMb = totalRamMb,
            availRamMb = mem.availMem / (1024 * 1024),
            lowMemory = mem.lowMemory,
            storageTotalGb = stat.totalBytes / 1e9,
            storageFreeGb = stat.availableBytes / 1e9,
            thermal = thermalLabel(),
            uptimeHours = SystemClock.elapsedRealtime() / 3_600_000L,
            unitClass = unitClassFor(totalRamMb),
            usageAccess = usage,
            busiestApps = busiest,
            unusedApps = unused,
            autoverseRoadDataMb = roadDataBytes / (1024 * 1024),
            autoverseMapCacheMb = File(app.filesDir, "mbgl-offline.db").length() / (1024 * 1024),
        )
    }

    private fun appUsage(): Pair<List<AppUse>, List<AppUse>> {
        val usm = app.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList<AppUse>() to emptyList()
        val pm = app.packageManager
        val now = System.currentTimeMillis()
        val day = usm.queryAndAggregateUsageStats(now - 24 * 3_600_000L, now)
        val month = usm.queryAndAggregateUsageStats(now - 60L * 24 * 3_600_000L, now)
        val launchable = try {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                .map { it.activityInfo.packageName }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
        fun use(pkg: String): AppUse? = try {
            val info = pm.getApplicationInfo(pkg, 0)
            val last = month[pkg]?.lastTimeUsed?.takeIf { it > 0 }
            AppUse(
                packageName = pkg,
                label = pm.getApplicationLabel(info).toString(),
                foregroundMinutes24h = (day[pkg]?.totalTimeInForeground ?: 0L) / 60_000L,
                lastUsedDaysAgo = last?.let { ((now - it) / 86_400_000L).toInt() },
                preinstalled = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            )
        } catch (_: Exception) {
            null
        }
        val busiest = day.values
            .filter { it.totalTimeInForeground > 60_000L && it.packageName != app.packageName }
            .sortedByDescending { it.totalTimeInForeground }
            .take(6)
            .mapNotNull { use(it.packageName) }
        val unused = launchable
            .filter { it != app.packageName }
            .mapNotNull(::use)
            .filter { (it.lastUsedDaysAgo ?: Int.MAX_VALUE) >= 30 }
            .sortedBy { it.label.lowercase() }
            .take(20)
        return busiest to unused
    }

    private fun readCpuMaxMhz(): Int? = try {
        (0 until Runtime.getRuntime().availableProcessors()).mapNotNull { i ->
            File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").takeIf { it.canRead() }?.readText()?.trim()?.toLongOrNull()
        }.maxOrNull()?.let { (it / 1000).toInt() }?.takeIf { it > 0 }
    } catch (_: Exception) {
        null
    }

    private fun thermalLabel(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val pm = app.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> "Normal"
            PowerManager.THERMAL_STATUS_MODERATE -> "warm"
            PowerManager.THERMAL_STATUS_SEVERE -> "hot"
            else -> "very hot"
        }
    }
}
