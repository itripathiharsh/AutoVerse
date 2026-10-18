package com.autoverse.launcher.core.design

import org.junit.Assert.assertEquals
import org.junit.Test

class LayoutClassTest {

    @Test
    fun `below 600dp is compact`() {
        assertEquals(LayoutClass.COMPACT, layoutClassFor(320))
        assertEquals(LayoutClass.COMPACT, layoutClassFor(599))
    }

    @Test
    fun `600dp starts medium`() {
        assertEquals(LayoutClass.MEDIUM, layoutClassFor(600))
        assertEquals(LayoutClass.MEDIUM, layoutClassFor(839))
    }

    @Test
    fun `840dp and above is expanded`() {
        assertEquals(LayoutClass.EXPANDED, layoutClassFor(840))
        assertEquals(LayoutClass.EXPANDED, layoutClassFor(1280))
    }
}
