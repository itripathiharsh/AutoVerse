package com.autoverse.launcher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerLayoutPolicyTest {

    @Test
    fun `apps split is 70 percent all apps and 30 percent recently used`() {
        assertEquals(0.7f, AppsLayoutRules.ALL_APPS_FRACTION, 0.0001f)
    }

    @Test
    fun `720p head unit content width keeps both panels side by side`() {
        assertTrue(appsPanelsSideBySide(733))
    }

    @Test
    fun `wide displays keep both panels side by side`() {
        assertTrue(appsPanelsSideBySide(1100))
    }

    @Test
    fun `phone width stacks recently used above the grid`() {
        assertFalse(appsPanelsSideBySide(303))
    }

    @Test
    fun `recent column minimum boundary`() {
        // 30% of 667 = 200.1 >= 200; 30% of 660 = 198 < 200.
        assertTrue(appsPanelsSideBySide(667))
        assertFalse(appsPanelsSideBySide(660))
    }
}
