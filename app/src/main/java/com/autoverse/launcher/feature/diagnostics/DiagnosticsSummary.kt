package com.autoverse.launcher.feature.diagnostics

import com.autoverse.launcher.core.diagnostics.AccessItem
import com.autoverse.launcher.core.diagnostics.AccessState
import com.autoverse.launcher.core.diagnostics.AppHealth
import com.autoverse.launcher.core.diagnostics.ComponentStatus
import com.autoverse.launcher.core.diagnostics.PermissionsSummary
import com.autoverse.launcher.core.diagnostics.grantedAccessCount
import com.autoverse.launcher.core.map.MapRenderSnapshot
import com.autoverse.launcher.core.model.AppearanceMode
import com.autoverse.launcher.core.model.PerformanceProfile
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Phase 17 — pure text mapping for the four summary cards. Android-free and
 * unit-tested so every card states exactly what the underlying state means,
 * with no invented counts or values.
 */

fun appStatusLabel(health: AppHealth): String = when (health) {
    AppHealth.NORMAL -> "Normal"
    AppHealth.PARTIAL -> "Starting"
    AppHealth.DEGRADED -> "Degraded"
}

fun appStatusDetail(health: AppHealth): String = when (health) {
    AppHealth.NORMAL -> "All core surfaces are working."
    AppHealth.PARTIAL -> "Some checks are still resolving."
    AppHealth.DEGRADED -> "Running with fallbacks — see components below."
}

/** The performance card leads with the profile actually in effect. */
fun profileSummaryText(effective: PerformanceProfile): String = effective.displayName

/**
 * Second line of the performance card explaining any divergence between the
 * stored choice and what is running, plus the two motion overrides that can
 * change rendering. Null when the card needs no note.
 */
fun profileNoteText(
    selected: PerformanceProfile,
    effective: PerformanceProfile,
    preferSmoothness: Boolean,
    reducedMotion: Boolean,
): String? {
    val notes = buildList {
        if (selected != effective) add("Selected: ${selected.displayName}")
        if (preferSmoothness && selected != PerformanceProfile.CONSTRAINED) {
            add("Prefer smoothness is on")
        }
        if (reducedMotion) add("Reduced motion is on")
    }
    return notes.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

fun appearanceDetailText(mode: AppearanceMode, effectiveDark: Boolean): String =
    "${mode.displayName} · ${if (effectiveDark) "dark" else "light"} theme"

fun permissionsSummaryLabel(summary: PermissionsSummary): String = when (summary) {
    PermissionsSummary.ALL_GOOD -> "All granted"
    PermissionsSummary.OPTIONAL_OFF -> "Optional off"
    PermissionsSummary.ACTION_NEEDED -> "Action needed"
}

/** "N of M granted", counting only accesses that apply on this platform. */
fun permissionsDetailText(items: List<AccessItem>): String {
    val applicable = items.count { it.state != AccessState.NOT_APPLICABLE }
    return "${grantedAccessCount(items)} of $applicable granted"
}

/**
 * Live FPS line for the performance panel. Keeps "tracking stopped" and
 * "no frame window yet" distinct so a paused tracker never reads as 0 FPS.
 */
fun fpsReadingText(fps: Float, tracking: Boolean): String = when {
    !tracking -> "Tracking disabled"
    fps <= 0f -> "Measuring…"
    else -> "${fps.roundToInt()} FPS"
}

/**
 * Live-map line: fps while the map was moving, slow frames and the worst
 * frame in the last window, plus whether that meets the smoothness bar.
 */
fun mapRenderReadingText(snapshot: MapRenderSnapshot): String = when {
    snapshot.totalFrames == 0L -> "Not rendered yet — open Home or Maps"
    snapshot.activeFps <= 0.0 -> "Idle (nothing moving)"
    else -> String.format(
        Locale.US,
        "%.0f fps · %d slow · worst %.0f ms · %s",
        snapshot.activeFps,
        snapshot.slowFrames,
        snapshot.worstFrameMs,
        if (snapshot.meetsTarget) "smooth" else "below target",
    )
}

/** Process CPU share of one core; null means the reading was unavailable. */
fun cpuReadingText(percentOfCore: Float?): String = if (percentOfCore == null) {
    "Not readable"
} else {
    String.format(Locale.US, "%.1f%% of one core", percentOfCore)
}

fun memoryReadingText(pssMb: Int?): String =
    if (pssMb == null) "Not readable" else "$pssMb MB"

fun deviceMemoryReadingText(availableMb: Int?, totalMb: Int?): String = when {
    availableMb == null || totalMb == null -> "Not readable"
    else -> "${megabytesText(availableMb)} free of ${megabytesText(totalMb)}"
}

fun storageReadingText(availableMb: Int?): String =
    if (availableMb == null) "Not readable" else "${megabytesText(availableMb)} free"

private fun megabytesText(mb: Int): String =
    if (mb >= 1024) String.format(Locale.US, "%.1f GB", mb / 1024.0) else "$mb MB"

fun componentStatusLabel(status: ComponentStatus): String = when (status) {
    ComponentStatus.READY -> "Ready"
    ComponentStatus.LOADING -> "Loading"
    ComponentStatus.PERMISSION_REQUIRED -> "Permission required"
    ComponentStatus.DISABLED -> "Disabled"
    ComponentStatus.UNAVAILABLE -> "Unavailable"
    ComponentStatus.ERROR -> "Error"
    ComponentStatus.NOT_CHECKED -> "Not checked"
}
