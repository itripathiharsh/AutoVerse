package com.autoverse.launcher.core.mapdisplay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDisplayHelpersTest {

    @Test
    fun `launch command targets the display and pins the maps package`() {
        val command = MapDisplayCommands.launchMapsOnDisplay(7)
        val text = command.joinToString(" ")
        assertTrue(text.contains("--display 7"))
        assertTrue(text.contains("-p ${MapDisplayCommands.MAPS_PACKAGE}"))
        assertTrue(text.contains("-a android.intent.action.VIEW"))
    }

    @Test
    fun `launch command carries the destination uri`() {
        val command = MapDisplayCommands.launchMapsOnDisplay(
            3,
            "geo:0,0?q=Vienna%20Airport",
        )
        assertTrue(command.contains("geo:0,0?q=Vienna%20Airport"))
    }

    @Test
    fun `fallback command omits the package pin`() {
        val command = MapDisplayCommands.launchGeoOnDisplay(5)
        val text = command.joinToString(" ")
        assertFalse(text.contains("-p"))
        assertTrue(text.contains("--display 5"))
    }

    @Test
    fun `touch coordinates scale with the display and clamp inside it`() {
        assertEquals(0f, TouchMapper.scaleX(0f, 400f, 800))
        assertEquals(800f, TouchMapper.scaleX(400f, 400f, 800))
        assertEquals(400f, TouchMapper.scaleY(200f, 400f, 800))
        assertEquals(800f, TouchMapper.scaleX(500f, 400f, 800)) // clamped
        assertEquals(0f, TouchMapper.scaleX(-10f, 400f, 800)) // clamped
    }

    @Test
    fun `degenerate sizes never divide by zero`() {
        assertEquals(0f, TouchMapper.scaleX(10f, 0f, 800))
        assertEquals(0f, TouchMapper.scaleX(10f, 400f, 0))
    }

    @Test
    fun `bounds check gates injection`() {
        assertTrue(TouchMapper.isInside(5f, 5f, 400f, 300f))
        assertFalse(TouchMapper.isInside(500f, 5f, 400f, 300f))
        assertFalse(TouchMapper.isInside(-1f, 5f, 400f, 300f))
    }
}
