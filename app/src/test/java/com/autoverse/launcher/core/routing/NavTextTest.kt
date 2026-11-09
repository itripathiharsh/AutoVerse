package com.autoverse.launcher.core.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class NavTextTest {
    private val left = Maneuver(0, ManeuverType.TURN_LEFT)
    private val right = Maneuver(0, ManeuverType.TURN_RIGHT)

    @Test
    fun `english prompts`() {
        assertEquals("In 500 metres, turn left", NavText.farPrompt(489.0, left, NavLanguage.ENGLISH))
        assertEquals("Turn left, then turn right", NavText.nearPrompt(left, right, NavLanguage.ENGLISH))
        assertEquals("Speed limit 50", NavText.speedLimit(50, NavLanguage.ENGLISH))
    }

    @Test
    fun `hindi prompts`() {
        assertEquals("500 मीटर बाद, बाएँ मुड़ें", NavText.farPrompt(489.0, left, NavLanguage.HINDI))
        assertEquals("बाएँ मुड़ें, फिर दाएँ मुड़ें", NavText.nearPrompt(left, right, NavLanguage.HINDI))
        assertEquals("1.5 किलोमीटर", NavText.spokenDistance(1500.0, NavLanguage.HINDI))
        assertEquals("गति सीमा 60", NavText.speedLimit(60, NavLanguage.HINDI))
        assertEquals("गोलचक्कर से 2वाँ निकास लें", NavText.maneuver(Maneuver(0, ManeuverType.ROUNDABOUT, 2), NavLanguage.HINDI))
    }
}
