package com.autoverse.launcher.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CockpitLayoutPolicyTest {

    @Test
    fun `720p head unit at 1_5x density gets the side-by-side cockpit, not a card grid`() {
        // 1280x720 @1.5x = 853x480dp; minus rail, gaps, paddings and top bar.
        val geometry = cockpitGeometryFor(contentWidthDp = 733, contentHeightDp = 400)
        assertEquals(CockpitArrangement.SIDE_BY_SIDE, geometry.arrangement)
        assertEquals(0.6f, geometry.mapFraction, 0.0001f)
    }

    @Test
    fun `1024x600 mdpi head unit is side by side`() {
        assertEquals(
            CockpitArrangement.SIDE_BY_SIDE,
            cockpitGeometryFor(contentWidthDp = 904, contentHeightDp = 520).arrangement,
        )
    }

    @Test
    fun `portrait phone stacks the map above music and speed`() {
        assertEquals(
            CockpitArrangement.STACKED,
            cockpitGeometryFor(contentWidthDp = 303, contentHeightDp = 800).arrangement,
        )
    }

    @Test
    fun `landscape too narrow for a usable side column stacks`() {
        // 40% of 500 = 200 < 220 minimum side column.
        assertEquals(
            CockpitArrangement.STACKED,
            cockpitGeometryFor(contentWidthDp = 500, contentHeightDp = 300).arrangement,
        )
    }

    @Test
    fun `side column boundary is inclusive`() {
        // 40% of 550 = 220 exactly.
        assertEquals(
            CockpitArrangement.SIDE_BY_SIDE,
            cockpitGeometryFor(contentWidthDp = 550, contentHeightDp = 400).arrangement,
        )
    }

    @Test
    fun `short head units use compact side panels, tall ones do not`() {
        assertTrue(cockpitGeometryFor(contentWidthDp = 733, contentHeightDp = 400).compactPanels)
        assertFalse(cockpitGeometryFor(contentWidthDp = 1100, contentHeightDp = 700).compactPanels)
    }

    @Test
    fun `degenerate sizes never crash and still produce a cockpit`() {
        val geometry = cockpitGeometryFor(contentWidthDp = 0, contentHeightDp = -5)
        assertEquals(CockpitArrangement.STACKED, geometry.arrangement)
    }
}
