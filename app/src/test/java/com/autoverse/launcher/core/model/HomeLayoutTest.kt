package com.autoverse.launcher.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `full screen is on by default and home shows every panel`() {
        val defaults = AppSettings()
        assertTrue(defaults.fullScreen)
        assertEquals(HomeLayout(showMap = true, showMusic = true, showSpeed = true), defaults.homeLayout)
        assertEquals(HomeMapSize.MEDIUM, defaults.homeLayout.mapSize)
    }

    @Test
    fun `turning every panel off brings the map back`() {
        val empty = HomeLayout(showMap = false, showMusic = false, showSpeed = false)
        assertTrue(empty.normalized().showMap)
    }

    @Test
    fun `a valid layout is kept as chosen`() {
        val musicOnly = HomeLayout(showMap = false, showMusic = true, showSpeed = false, mapOnRight = true)
        assertEquals(musicOnly, musicOnly.normalized())
    }

    @Test
    fun `map sizes are 50, 60 and 70 percent`() {
        assertEquals(listOf(0.5f, 0.6f, 0.7f), HomeMapSize.entries.map { it.fraction })
    }
}
