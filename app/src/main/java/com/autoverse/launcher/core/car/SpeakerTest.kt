package com.autoverse.launcher.core.car

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/** The extra whole-system tones (per-speaker tones are [SpeakerPosition]). */
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
 *
 * Most head units give apps a stereo output and do front/rear balance in
 * their own DSP (the fader). [detect] only reports a multichannel output when
 * the unit's speaker output says it has 4+ channels and a track in that
 * layout opens; then each speaker gets its own channel. Otherwise rear
 * speakers need the unit's fader (see [SpeakerRoute.needsFader]).
 */
object SpeakerTest {
    private const val RATE = 44_100
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    /** Output device types that feed the cabin speakers (not HDMI, USB, Bluetooth, headsets). */
    private val CABIN_OUTPUT_TYPES = setOf(
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_LINE_ANALOG,
        AudioDeviceInfo.TYPE_AUX_LINE,
        21, // AudioDeviceInfo.TYPE_BUS (API 24): Android Automotive amplifier buses
        24, // AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE (API 30)
    )

    /** Works out whether speakers can be addressed directly. Never throws. */
    suspend fun detect(context: Context): SpeakerOutputInfo = withContext(Dispatchers.Default) {
        val reported = reportedCabinChannels(context)
        val output = chooseSpeakerOutput(reported) { canOpen(it.channelMask) }
        SpeakerOutputInfo(output, reported)
    }

    private fun reportedCabinChannels(context: Context): Int = try {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { it.isSink && it.type in CABIN_OUTPUT_TYPES }
            .maxOfOrNull { reportedChannelCount(it.channelCounts, it.channelMasks) } ?: 0
    } catch (_: Exception) {
        0
    }

    private fun canOpen(mask: Int): Boolean {
        val min = AudioTrack.getMinBufferSize(RATE, mask, ENCODING)
        if (min <= 0) return false
        var track: AudioTrack? = null
        return try {
            track = buildTrack(mask, min)
            track.state == AudioTrack.STATE_INITIALIZED
        } catch (_: Exception) {
            false
        } finally {
            track?.release()
        }
    }

    /**
     * Plays one speaker's tone. Falls back to stereo if the direct layout
     * fails to open. Returns the output actually used.
     */
    suspend fun playSpeaker(position: SpeakerPosition, output: SpeakerOutput, seconds: Double): SpeakerOutput {
        if (output.isDirect) {
            try {
                playPcm(renderRoute(speakerRoute(position, output), seconds, RATE), output.channelMask)
                return output
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The unit refused the multichannel track: carry on in stereo.
            }
        }
        val stereo = SpeakerOutput.STEREO
        playPcm(renderRoute(speakerRoute(position, stereo), seconds, RATE), stereo.channelMask)
        return stereo
    }

    /** One of the extra stereo tones (sweep, phase, ...). */
    suspend fun play(tone: SpeakerTone) {
        val seconds = if (tone == SpeakerTone.SWEEP) 8.0 else 2.5
        playPcm(render(tone, seconds), SpeakerChannels.STEREO)
    }

    /**
     * Streams interleaved PCM in small chunks so cancelling stops it within a
     * few tens of milliseconds. Always stops and releases the track.
     */
    private suspend fun playPcm(pcm: ShortArray, channelMask: Int) = withContext(Dispatchers.Default) {
        val channels = SpeakerChannels.count(channelMask)
        val min = AudioTrack.getMinBufferSize(RATE, channelMask, ENCODING)
        check(min > 0) { "Channel layout not supported" }
        val bufferBytes = maxOf(min, RATE / 10 * channels * 2) // about 100 ms
        val track = buildTrack(channelMask, bufferBytes)
        try {
            check(track.state == AudioTrack.STATE_INITIALIZED) { "AudioTrack not initialised" }
            track.play()
            val chunk = RATE / 50 * channels // 20 ms, a whole number of frames
            var offset = 0
            while (offset < pcm.size) {
                ensureActive()
                val written = track.write(pcm, offset, minOf(chunk, pcm.size - offset))
                if (written <= 0) break
                offset += written
            }
            // Let the buffered tail play out (it is already faded to silence).
            val totalFrames = pcm.size / channels
            val deadline = System.currentTimeMillis() + 1_000
            while (track.playbackHeadPosition < totalFrames && System.currentTimeMillis() < deadline) {
                ensureActive()
                delay(20)
            }
        } finally {
            runCatching {
                track.pause()
                track.flush()
            }
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun buildTrack(channelMask: Int, bufferBytes: Int): AudioTrack =
        AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(ENCODING)
                    .setSampleRate(RATE)
                    .setChannelMask(channelMask)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(bufferBytes)
            .build()

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
