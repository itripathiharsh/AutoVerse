package com.autoverse.launcher.core.performance

import com.autoverse.launcher.core.design.StatusChipsMode
import com.autoverse.launcher.core.model.PerformanceProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 9 — the profile-to-behavior table. Every knob the UI reads must be
 * pinned here: Balanced is the pre-Phase-9 behavior byte for byte,
 * Constrained strips optional rendering, and "Prefer smoothness" always
 * resolves to the Constrained workload regardless of the stored profile.
 */
class PerformanceSpecTest {

    @Test
    fun `constrained spec is the least rendering work`() {
        val spec = PerformanceSpec.Constrained
        assertEquals(PerformanceProfile.CONSTRAINED, spec.profile)
        assertEquals(ScreenTransitionStyle.NONE, spec.screenTransitions)
        assertTrue(spec.flatSurfaces)
        assertFalse(spec.showFpsReadout)
        assertTrue(spec.minuteAlignedClock)
        assertEquals(128, spec.artworkTargetPx)
        assertFalse(spec.artworkCrossfade)
        assertEquals(StatusChipsMode.REDUCED, spec.maxStatusChips)
    }

    @Test
    fun `balanced spec preserves the pre phase 9 behavior`() {
        val spec = PerformanceSpec.Balanced
        assertEquals(PerformanceProfile.BALANCED, spec.profile)
        assertEquals(ScreenTransitionStyle.FADE, spec.screenTransitions)
        assertFalse(spec.flatSurfaces)
        assertTrue(spec.showFpsReadout)
        assertFalse(spec.minuteAlignedClock)
        assertEquals(256, spec.artworkTargetPx)
        assertFalse(spec.artworkCrossfade)
        assertEquals(StatusChipsMode.FULL, spec.maxStatusChips)
    }

    @Test
    fun `enhanced spec adds transitions and artwork crossfades only`() {
        val spec = PerformanceSpec.Enhanced
        assertEquals(PerformanceProfile.ENHANCED, spec.profile)
        assertEquals(ScreenTransitionStyle.RICH, spec.screenTransitions)
        assertFalse(spec.flatSurfaces)
        assertTrue(spec.showFpsReadout)
        assertFalse(spec.minuteAlignedClock)
        assertEquals(256, spec.artworkTargetPx)
        assertTrue(spec.artworkCrossfade)
        assertEquals(StatusChipsMode.FULL, spec.maxStatusChips)
    }

    @Test
    fun `default spec is balanced`() {
        assertEquals(PerformanceSpec.Balanced, PerformanceSpec.Default)
        assertEquals(PerformanceProfile.BALANCED, PerformanceSpec.Default.profile)
    }

    @Test
    fun `every artwork target is a power of two`() {
        listOf(PerformanceSpec.Constrained, PerformanceSpec.Balanced, PerformanceSpec.Enhanced)
            .forEach { spec ->
                val target = spec.artworkTargetPx
                assertTrue(
                    "target=$target",
                    target > 0 && (target and (target - 1)) == 0,
                )
            }
    }

    @Test
    fun `spec mapping follows the stored profile without the override`() {
        assertEquals(PerformanceSpec.Constrained, performanceSpecFor(PerformanceProfile.CONSTRAINED, false))
        assertEquals(PerformanceSpec.Balanced, performanceSpecFor(PerformanceProfile.BALANCED, false))
        assertEquals(PerformanceSpec.Enhanced, performanceSpecFor(PerformanceProfile.ENHANCED, false))
    }

    @Test
    fun `prefer smoothness forces the constrained workload for every profile`() {
        PerformanceProfile.entries.forEach { profile ->
            assertEquals(
                "profile=$profile",
                PerformanceProfile.CONSTRAINED,
                effectiveWorkloadProfile(profile, preferSmoothness = true),
            )
            assertEquals(
                "profile=$profile",
                PerformanceSpec.Constrained,
                performanceSpecFor(profile, preferSmoothness = true),
            )
        }
    }

    @Test
    fun `the override leaves the stored profile untouched when off`() {
        PerformanceProfile.entries.forEach { profile ->
            assertEquals(profile, effectiveWorkloadProfile(profile, preferSmoothness = false))
            assertEquals(profile, performanceSpecFor(profile, preferSmoothness = false).profile)
        }
    }
}
