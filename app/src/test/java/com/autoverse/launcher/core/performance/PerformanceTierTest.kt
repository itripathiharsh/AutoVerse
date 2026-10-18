package com.autoverse.launcher.core.performance

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceTierTest {

    @Test
    fun `low ram device is constrained regardless of specs`() {
        assertEquals(
            PerformanceTier.CONSTRAINED,
            performanceTierFor(totalRamMb = 8_192, cpuCores = 8, lowRamDevice = true),
        )
    }

    @Test
    fun `small memory is constrained`() {
        assertEquals(
            PerformanceTier.CONSTRAINED,
            performanceTierFor(totalRamMb = 1_536, cpuCores = 8, lowRamDevice = false),
        )
    }

    @Test
    fun `few cores are constrained`() {
        assertEquals(
            PerformanceTier.CONSTRAINED,
            performanceTierFor(totalRamMb = 8_192, cpuCores = 2, lowRamDevice = false),
        )
    }

    @Test
    fun `a mid range head unit is balanced`() {
        assertEquals(
            PerformanceTier.BALANCED,
            performanceTierFor(totalRamMb = 3_072, cpuCores = 4, lowRamDevice = false),
        )
    }

    @Test
    fun `a capable head unit is enhanced`() {
        assertEquals(
            PerformanceTier.ENHANCED,
            performanceTierFor(totalRamMb = 8_192, cpuCores = 8, lowRamDevice = false),
        )
        assertEquals(
            PerformanceTier.ENHANCED,
            performanceTierFor(totalRamMb = 4_096, cpuCores = 6, lowRamDevice = false),
        )
    }
}
