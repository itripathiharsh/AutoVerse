package com.autoverse.launcher.core.design

import com.autoverse.launcher.core.design.tokens.MotionSpec
import com.autoverse.launcher.core.design.tokens.effectiveReducedMotion
import com.autoverse.launcher.core.design.tokens.motionSpecFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {

    @Test
    fun `default durations are ordered fast to emphasized`() {
        val spec = MotionSpec.Default
        assertTrue(spec.fastMs < spec.standardMs)
        assertTrue(spec.standardMs <= spec.emphasizedMs)
    }

    @Test
    fun `total duration stays within the calm-cabin budget`() {
        assertTrue(MotionSpec.Default.emphasizedMs <= 250)
    }

    @Test
    fun `reduced motion removes all durations`() {
        assertEquals(0, MotionSpec.Reduced.fastMs)
        assertEquals(0, MotionSpec.Reduced.standardMs)
        assertEquals(0, MotionSpec.Reduced.emphasizedMs)
    }

    @Test
    fun `motionSpecFor selects by flag`() {
        assertEquals(MotionSpec.Default, motionSpecFor(reducedMotion = false))
        assertEquals(MotionSpec.Reduced, motionSpecFor(reducedMotion = true))
    }

    @Test
    fun `effective reduced motion is the union of app setting and system scale`() {
        assertFalse(effectiveReducedMotion(userSetting = false, systemAnimationsDisabled = false))
        assertTrue(effectiveReducedMotion(userSetting = true, systemAnimationsDisabled = false))
        assertTrue(effectiveReducedMotion(userSetting = false, systemAnimationsDisabled = true))
        assertTrue(effectiveReducedMotion(userSetting = true, systemAnimationsDisabled = true))
    }
}
