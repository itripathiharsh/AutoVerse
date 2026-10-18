package com.autoverse.launcher.core.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlin.math.roundToInt

data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val totalRamMb: Int,
    val cpuCores: Int,
    val lowRamDevice: Boolean,
    val densityDpi: Int,
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val tier: PerformanceTier,
) {
    companion object {
        private const val MEGABYTE = 1024L * 1024L

        fun sample(context: Context): DeviceProfile {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
            val metrics = context.resources.displayMetrics
            val density = if (metrics.density > 0f) metrics.density else 1f
            val totalRamMb = (memoryInfo.totalMem / MEGABYTE).toInt()
            val cpuCores = Runtime.getRuntime().availableProcessors()
            return DeviceProfile(
                manufacturer = Build.MANUFACTURER,
                model = Build.MODEL,
                androidRelease = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                totalRamMb = totalRamMb,
                cpuCores = cpuCores,
                lowRamDevice = activityManager.isLowRamDevice,
                densityDpi = metrics.densityDpi,
                screenWidthDp = (metrics.widthPixels / density).roundToInt(),
                screenHeightDp = (metrics.heightPixels / density).roundToInt(),
                tier = performanceTierFor(totalRamMb, cpuCores, activityManager.isLowRamDevice),
            )
        }
    }
}
