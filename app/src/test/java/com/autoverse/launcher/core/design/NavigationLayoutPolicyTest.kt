package com.autoverse.launcher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationLayoutPolicyTest {

    @Test
    fun `maps split is 70 percent map and 30 percent destinations`() {
        assertEquals(0.7f, MapsLayoutRules.MAP_FRACTION, 0.0001f)
    }

    @Test
    fun `720p head unit content width keeps map and destinations side by side`() {
        assertTrue(mapsPanelsSideBySide(733))
    }

    @Test
    fun `phone width stacks destinations under the map`() {
        assertFalse(mapsPanelsSideBySide(303))
    }

    @Test
    fun `destinations column minimum boundary`() {
        assertTrue(mapsPanelsSideBySide(667))
        assertFalse(mapsPanelsSideBySide(660))
    }
}
