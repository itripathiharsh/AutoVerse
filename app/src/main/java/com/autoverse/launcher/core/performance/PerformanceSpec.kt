package com.autoverse.launcher.core.performance

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.autoverse.launcher.core.design.StatusChipsMode
import com.autoverse.launcher.core.model.PerformanceProfile

/** How the app moves between screens under the active workload profile. */
enum class ScreenTransitionStyle { NONE, FADE, RICH }

/**
 * Phase 9 — the observable behavior contract behind each performance
 * profile. Every field changes real rendering work; the mapping is pure and
 * table-tested (PerformanceSpecTest). Beta rule: Balanced is byte-for-byte
 * the Phase 1-8 behavior, Constrained strips optional rendering, Enhanced
 * adds polish only where a head unit has not yet been profiled.
 */
@Immutable
data class PerformanceSpec(
    val profile: PerformanceProfile,
    val screenTransitions: ScreenTransitionStyle,
    val flatSurfaces: Boolean,
    val showFpsReadout: Boolean,
    val minuteAlignedClock: Boolean,
    val artworkTargetPx: Int,
    val artworkCrossfade: Boolean,
    val maxStatusChips: StatusChipsMode,
) {
    companion object {

        /** Least rendering work: no transitions, flat surfaces, no live readouts. */
        val Constrained = PerformanceSpec(
            profile = PerformanceProfile.CONSTRAINED,
            screenTransitions = ScreenTransitionStyle.NONE,
            flatSurfaces = true,
            showFpsReadout = false,
            minuteAlignedClock = true,
            artworkTargetPx = 128,
            artworkCrossfade = false,
            maxStatusChips = StatusChipsMode.REDUCED,
        )

        /** Identical to the pre-Phase-9 behavior; the out-of-the-box workload. */
        val Balanced = PerformanceSpec(
            profile = PerformanceProfile.BALANCED,
            screenTransitions = ScreenTransitionStyle.FADE,
            flatSurfaces = false,
            showFpsReadout = true,
            minuteAlignedClock = false,
            artworkTargetPx = 256,
            artworkCrossfade = false,
            maxStatusChips = StatusChipsMode.FULL,
        )

        /** Balanced plus fade + slide transitions and artwork crossfades. */
        val Enhanced = PerformanceSpec(
            profile = PerformanceProfile.ENHANCED,
            screenTransitions = ScreenTransitionStyle.RICH,
            flatSurfaces = false,
            showFpsReadout = true,
            minuteAlignedClock = false,
            artworkTargetPx = 256,
            artworkCrossfade = true,
            maxStatusChips = StatusChipsMode.FULL,
        )

        val Default: PerformanceSpec = Balanced
    }
}

/** "Prefer smoothness" always resolves to the Constrained workload. */
fun effectiveWorkloadProfile(
    profile: PerformanceProfile,
    preferSmoothness: Boolean,
): PerformanceProfile =
    if (preferSmoothness) PerformanceProfile.CONSTRAINED else profile

/**
 * Full behavior spec for the stored profile plus the "Prefer smoothness"
 * override; the single mapping point between persisted settings and runtime
 * rendering behavior.
 */
fun performanceSpecFor(
    profile: PerformanceProfile,
    preferSmoothness: Boolean,
): PerformanceSpec =
    when (effectiveWorkloadProfile(profile, preferSmoothness)) {
        PerformanceProfile.CONSTRAINED -> PerformanceSpec.Constrained
        PerformanceProfile.BALANCED -> PerformanceSpec.Balanced
        PerformanceProfile.ENHANCED -> PerformanceSpec.Enhanced
    }

/** Behavior contract for the current app surface; previews fall back to [PerformanceSpec.Default]. */
val LocalPerformanceSpec = staticCompositionLocalOf { PerformanceSpec.Default }
