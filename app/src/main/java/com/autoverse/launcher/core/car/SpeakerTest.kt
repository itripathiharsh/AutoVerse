package com.autoverse.launcher.core.car

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/** What the speaker test plays. */
enum class SpeakerTone(val label: String, val detail: String) {
    LEFT("Left", "Voice tone on the left speakers only"),
    RIGHT("Right", "Voice tone on the right speakers only"),
    BOTH("Both", "Same tone on both sides — should sound centred"),
    BASS("Bass", "Low 50 Hz tone — checks the woofer and door rattles"),
    SWEEP("Sweep", "Slides from deep bass to high treble — listen for buzzes and dead spots"),
    PHASE("Phase", "Left and right in opposite phase — sounds thin if a speaker is wired backwards"),
}

/**
 * Generates test tones in memory and plays them through the media stream.
 * Front/rear fade is controlled by the head unit's own sound settings on
 * most units, so the test covers left/right, bass, sweep and phase.
 */
object SpeakerTest {
    private const val RATE = 44_100

    suspend fun play(tone: SpeakerTone) = withContext(Dispatchers.Default) {
        val seconds = if (tone == SpeakerTone.SWEEP) 8.0 else 2.5
        val pcm = render(tone, seconds)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            val endAt = System.currentTimeMillis() + (seconds * 1000).toLong()
            while (System.currentTimeMillis() < endAt) {
                coroutineContext.ensureActive()
                kotlinx.coroutines.delay(50)
            }
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    /** Interleaved stereo 16-bit samples. Pure, so the waveform is unit-testable. */
    fun render(tone: SpeakerTone, seconds: Double): ShortArray {
        val frames = (RATE * seconds).toInt()
        val out = ShortArray(frames * 2)
        val amp = 0.45 * Short.MAX_VALUE
        var phase = 0.0
        for (i in 0 until frames) {
            val t = i.toDouble() / RATE
            val freq = when (tone) {
                SpeakerTone.BASS -> 50.0
                SpeakerTone.SWEEP -> 40.0 * exp(ln(15_000.0 / 40.0) * t / seconds)
                else -> 440.0
            }
            phase += 2 * PI * freq / RATE
            // 30 ms fade in/out so the speakers never click.
            val edge = minOf(t, seconds - t)
            val env = (edge / 0.03).coerceIn(0.0, 1.0)
            val s = sin(phase) * amp * env
            val (l, r) = when (tone) {
                SpeakerTone.LEFT -> s to 0.0
                SpeakerTone.RIGHT -> 0.0 to s
                SpeakerTone.PHASE -> s to -s
                else -> s to s
            }
            out[2 * i] = l.toInt().toShort()
            out[2 * i + 1] = r.toInt().toShort()
        }
        return out
    }
}
