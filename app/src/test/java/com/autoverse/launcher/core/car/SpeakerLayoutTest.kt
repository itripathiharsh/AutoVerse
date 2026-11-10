package com.autoverse.launcher.core.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeakerLayoutTest {

    @Test
    fun `layouts list their speakers clockwise from front left`() {
        assertEquals(
            listOf(SpeakerPosition.FRONT_LEFT, SpeakerPosition.FRONT_RIGHT),
            SpeakerLayout.TWO.walkOrder,
        )
        assertEquals(
            listOf(SpeakerPosition.FRONT_LEFT, SpeakerPosition.FRONT_RIGHT, SpeakerPosition.REAR_RIGHT, SpeakerPosition.REAR_LEFT),
            SpeakerLayout.FOUR.walkOrder,
        )
        assertEquals(
            listOf(
                SpeakerPosition.FRONT_LEFT, SpeakerPosition.DASH_LEFT, SpeakerPosition.DASH_RIGHT,
                SpeakerPosition.FRONT_RIGHT, SpeakerPosition.REAR_RIGHT, SpeakerPosition.SUBWOOFER,
                SpeakerPosition.REAR_LEFT,
            ),
            SpeakerLayout.SIX_SUB.walkOrder,
        )
        assertEquals(5, SpeakerLayout.FOUR_SUB.walkOrder.size)
        assertEquals(6, SpeakerLayout.SIX.walkOrder.size)
        // Every speaker appears once.
        SpeakerLayout.entries.forEach { assertEquals(it.walkOrder.size, it.walkOrder.toSet().size) }
    }

    @Test
    fun `layout flags and remembered name`() {
        assertFalse(SpeakerLayout.TWO.hasRear)
        assertTrue(SpeakerLayout.FOUR.hasRear)
        assertFalse(SpeakerLayout.FOUR.hasSub)
        assertTrue(SpeakerLayout.FOUR_SUB.hasSub)
        assertTrue(SpeakerLayout.SIX.hasDash)
        assertFalse(SpeakerLayout.FOUR_SUB.hasDash)
        assertEquals(SpeakerLayout.SIX_SUB, SpeakerLayout.fromName("SIX_SUB"))
        assertEquals(SpeakerLayout.DEFAULT, SpeakerLayout.fromName(null))
        assertEquals(SpeakerLayout.DEFAULT, SpeakerLayout.fromName("EIGHT"))
    }

    @Test
    fun `channel masks match Android and interleave in bit order`() {
        assertEquals(12, SpeakerChannels.STEREO)          // AudioFormat.CHANNEL_OUT_STEREO
        assertEquals(204, SpeakerChannels.QUAD)           // AudioFormat.CHANNEL_OUT_QUAD
        assertEquals(252, SpeakerChannels.FIVE_POINT_ONE) // AudioFormat.CHANNEL_OUT_5POINT1
        assertEquals(2, SpeakerOutput.STEREO.channels)
        assertEquals(4, SpeakerOutput.QUAD.channels)
        assertEquals(6, SpeakerOutput.FIVE_POINT_ONE.channels)

        val quad = SpeakerChannels.QUAD
        assertEquals(0, SpeakerChannels.indexOf(quad, SpeakerChannels.FRONT_LEFT))
        assertEquals(1, SpeakerChannels.indexOf(quad, SpeakerChannels.FRONT_RIGHT))
        assertEquals(2, SpeakerChannels.indexOf(quad, SpeakerChannels.BACK_LEFT))
        assertEquals(3, SpeakerChannels.indexOf(quad, SpeakerChannels.BACK_RIGHT))
        assertEquals(-1, SpeakerChannels.indexOf(quad, SpeakerChannels.LOW_FREQUENCY))

        val surround = SpeakerChannels.FIVE_POINT_ONE
        assertEquals(2, SpeakerChannels.indexOf(surround, SpeakerChannels.FRONT_CENTER))
        assertEquals(3, SpeakerChannels.indexOf(surround, SpeakerChannels.LOW_FREQUENCY))
        assertEquals(4, SpeakerChannels.indexOf(surround, SpeakerChannels.BACK_LEFT))
        assertEquals(5, SpeakerChannels.indexOf(surround, SpeakerChannels.BACK_RIGHT))
    }

    @Test
    fun `multichannel only when the device reports it and the track opens`() {
        val opensAll: (SpeakerOutput) -> Boolean = { true }
        val opensNone: (SpeakerOutput) -> Boolean = { false }
        assertEquals(SpeakerOutput.STEREO, chooseSpeakerOutput(2, opensAll))   // mixer would fold quad down
        assertEquals(SpeakerOutput.STEREO, chooseSpeakerOutput(0, opensAll))   // device did not say
        assertEquals(SpeakerOutput.QUAD, chooseSpeakerOutput(4, opensAll))
        assertEquals(SpeakerOutput.FIVE_POINT_ONE, chooseSpeakerOutput(8, opensAll))
        assertEquals(SpeakerOutput.QUAD, chooseSpeakerOutput(6) { it == SpeakerOutput.QUAD })
        assertEquals(SpeakerOutput.STEREO, chooseSpeakerOutput(6, opensNone))
    }

    @Test
    fun `reported channel count from counts and masks`() {
        assertEquals(0, reportedChannelCount(intArrayOf(), intArrayOf()))
        assertEquals(2, reportedChannelCount(intArrayOf(1, 2), intArrayOf()))
        assertEquals(4, reportedChannelCount(intArrayOf(2), intArrayOf(SpeakerChannels.STEREO, SpeakerChannels.QUAD)))
        assertEquals(6, reportedChannelCount(intArrayOf(), intArrayOf(SpeakerChannels.FIVE_POINT_ONE)))
    }

    @Test
    fun `stereo unit routes rear speakers to left and right and asks for the fader`() {
        val out = SpeakerOutput.STEREO
        assertEquals(setOf(0), speakerRoute(SpeakerPosition.FRONT_LEFT, out).channels)
        assertEquals(setOf(1), speakerRoute(SpeakerPosition.FRONT_RIGHT, out).channels)
        val rearLeft = speakerRoute(SpeakerPosition.REAR_LEFT, out)
        assertEquals(setOf(0), rearLeft.channels)
        assertTrue(rearLeft.needsFader)
        assertTrue(speakerRoute(SpeakerPosition.REAR_RIGHT, out).needsFader)
        assertFalse(speakerRoute(SpeakerPosition.FRONT_LEFT, out).needsFader)
        val sub = speakerRoute(SpeakerPosition.SUBWOOFER, out)
        assertEquals(setOf(0, 1), sub.channels)
        assertTrue(sub.frequencyHz in 45.0..60.0)
        assertFalse(sub.needsFader)
    }

    @Test
    fun `direct output puts each speaker on its own channel`() {
        val quad = SpeakerOutput.QUAD
        assertEquals(setOf(2), speakerRoute(SpeakerPosition.REAR_LEFT, quad).channels)
        assertEquals(setOf(3), speakerRoute(SpeakerPosition.REAR_RIGHT, quad).channels)
        assertFalse(speakerRoute(SpeakerPosition.REAR_LEFT, quad).needsFader)
        assertEquals(setOf(0, 1, 2, 3), speakerRoute(SpeakerPosition.SUBWOOFER, quad).channels)

        val surround = SpeakerOutput.FIVE_POINT_ONE
        assertEquals(setOf(3), speakerRoute(SpeakerPosition.SUBWOOFER, surround).channels)
        assertEquals(setOf(4), speakerRoute(SpeakerPosition.REAR_LEFT, surround).channels)
        assertEquals(setOf(5), speakerRoute(SpeakerPosition.REAR_RIGHT, surround).channels)

        // Tweeters share the front channel but use a high tone.
        val tweeter = speakerRoute(SpeakerPosition.DASH_RIGHT, quad)
        assertEquals(setOf(1), tweeter.channels)
        assertTrue(tweeter.frequencyHz > 2_000.0)
    }

    @Test
    fun `interleave writes only the chosen channel`() {
        val mono = shortArrayOf(100, -200, 300)
        val out = interleave(mono, channels = 4, active = setOf(2))
        assertEquals(12, out.size)
        for (frame in mono.indices) {
            for (c in 0 until 4) {
                val expected = if (c == 2) mono[frame].toInt() else 0
                assertEquals(expected, out[frame * 4 + c].toInt())
            }
        }
        val both = interleave(mono, channels = 2, active = setOf(0, 1))
        assertEquals(listOf<Short>(100, 100, -200, -200, 300, 300), both.toList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `interleave rejects a channel outside the frame`() {
        interleave(shortArrayOf(1), channels = 2, active = setOf(2))
    }

    @Test
    fun `tones fade in and out and stay gentle`() {
        val rate = 44_100
        val tone = toneSamples(440.0, 0.5, rate, amplitude = 0.35)
        assertEquals(rate / 2, tone.size)
        assertEquals(0, tone.first().toInt())
        assertTrue(kotlin.math.abs(tone.last().toInt()) < 200)
        val peak = tone.maxOf { kotlin.math.abs(it.toInt()) }
        assertTrue(peak > 0.3 * Short.MAX_VALUE)
        assertTrue(peak <= 0.35 * Short.MAX_VALUE + 1)

        val route = speakerRoute(SpeakerPosition.REAR_RIGHT, SpeakerOutput.FIVE_POINT_ONE)
        val pcm = renderRoute(route, 0.1, rate)
        val frames = (rate * 0.1).toInt()
        assertEquals(frames * 6, pcm.size)
        assertTrue((0 until frames).all { f -> (0 until 5).all { c -> pcm[f * 6 + c].toInt() == 0 } })
        assertTrue((0 until frames).any { f -> pcm[f * 6 + 5].toInt() != 0 })
    }

    @Test
    fun `mode line says which routing is active`() {
        assertEquals("Direct speaker output (4 channels)", SpeakerOutputInfo(SpeakerOutput.QUAD, 4).summary)
        assertEquals("Stereo unit — use the fader for front/rear", SpeakerOutputInfo(SpeakerOutput.STEREO, 2).summary)
    }

    @Test
    fun `sound settings app names`() {
        assertTrue(looksLikeSoundSettings("com.syu.eq", "com.syu.eq.MainActivity"))
        assertTrue(looksLikeSoundSettings("com.example.carsettings", "com.example.carsettings.SoundSettingsActivity"))
        assertFalse(looksLikeSoundSettings("com.tw.music", "com.tw.music.MusicActivity"))
        assertFalse(looksLikeSoundSettings("com.soundcloud.android", "com.soundcloud.android.MainActivity"))
    }
}
