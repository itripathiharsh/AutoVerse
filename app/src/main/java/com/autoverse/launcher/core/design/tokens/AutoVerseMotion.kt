package com.autoverse.launcher.core.design.tokens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class MotionSpec(
    val fastMs: Int,
    val standardMs: Int,
    val emphasizedMs: Int,
) {
    companion object {
        val Default = MotionSpec(fastMs = 120, standardMs = 180, emphasizedMs = 220)
        val Reduced = MotionSpec(fastMs = 0, standardMs = 0, emphasizedMs = 0)
    }
}

fun motionSpecFor(reducedMotion: Boolean): MotionSpec =
    if (reducedMotion) MotionSpec.Reduced else MotionSpec.Default

/**
 * A single decision point for reduced motion. Motion is disabled when either
 * the in-app AutoVerse preference is on or the system-wide "remove animations"
 * setting is active (Android animator duration scale of zero — the platform's
 * accessibility control for motion). Keeping this pure makes the combination
 * rule unit-testable and documents the intent in one place.
 */
fun effectiveReducedMotion(userSetting: Boolean, systemAnimationsDisabled: Boolean): Boolean =
    userSetting || systemAnimationsDisabled

val LocalAutoVerseMotion = staticCompositionLocalOf { MotionSpec.Default }

object AutoVerseEasing {
    val standard = FastOutSlowInEasing
    val linear = LinearEasing
}
