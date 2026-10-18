package com.autoverse.launcher.core.performance

enum class PerformanceTier(val displayName: String) {
    CONSTRAINED("Constrained"),
    BALANCED("Balanced"),
    ENHANCED("Enhanced"),
}

fun performanceTierFor(totalRamMb: Int, cpuCores: Int, lowRamDevice: Boolean): PerformanceTier = when {
    lowRamDevice -> PerformanceTier.CONSTRAINED
    totalRamMb < 2048 || cpuCores <= 2 -> PerformanceTier.CONSTRAINED
    totalRamMb >= 4096 && cpuCores >= 6 -> PerformanceTier.ENHANCED
    else -> PerformanceTier.BALANCED
}
