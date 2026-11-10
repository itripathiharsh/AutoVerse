package com.autoverse.launcher.core.car

import kotlin.math.PI
import kotlin.math.sin

/*
 * Pure speaker-test logic (no Android types), so it runs in JVM unit tests:
 * which speakers a car has, the order "Play all in turn" walks them, which
 * output channel each one maps to, and the interleaved PCM for a tone.
 */

/** Where a speaker sits in the cabin. */
enum class SpeakerPosition(val label: String, val shortLabel: String) {
    FRONT_LEFT("Front left", "Front L"),
    FRONT_RIGHT("Front right", "Front R"),
    REAR_LEFT("Rear left", "Rear L"),
    REAR_RIGHT("Rear right", "Rear R"),
    DASH_LEFT("Tweeter left", "Tweeter L"),
    DASH_RIGHT("Tweeter right", "Tweeter R"),
    SUBWOOFER("Subwoofer", "Subwoofer"),
    ;

    val isRear: Boolean get() = this == REAR_LEFT || this == REAR_RIGHT
}

/**
 * The speaker sets a car commonly has. [walkOrder] goes clockwise seen from
 * above with the bonnet at the top: up the left side, across the dash, down
 * the right side, across the boot and back up the left.
 */
enum class SpeakerLayout(val label: String, val walkOrder: List<SpeakerPosition>) {
    TWO(
        "2 speakers",
        listOf(SpeakerPosition.FRONT_LEFT, SpeakerPosition.FRONT_RIGHT),
    ),
    FOUR(
        "4 speakers",
        listOf(SpeakerPosition.FRONT_LEFT, SpeakerPosition.FRONT_RIGHT, SpeakerPosition.REAR_RIGHT, SpeakerPosition.REAR_LEFT),
    ),
    FOUR_SUB(
        "4 + subwoofer",
        listOf(
            SpeakerPosition.FRONT_LEFT, SpeakerPosition.FRONT_RIGHT, SpeakerPosition.REAR_RIGHT,
            SpeakerPosition.SUBWOOFER, SpeakerPosition.REAR_LEFT,
        ),
    ),
    SIX(
        "6 speakers",
        listOf(
            SpeakerPosition.FRONT_LEFT, SpeakerPosition.DASH_LEFT, SpeakerPosition.DASH_RIGHT,
            SpeakerPosition.FRONT_RIGHT, SpeakerPosition.REAR_RIGHT, SpeakerPosition.REAR_LEFT,
        ),
    ),
    SIX_SUB(
        "6 + subwoofer",
        listOf(
            SpeakerPosition.FRONT_LEFT, SpeakerPosition.DASH_LEFT, SpeakerPosition.DASH_RIGHT,
            SpeakerPosition.FRONT_RIGHT, SpeakerPosition.REAR_RIGHT, SpeakerPosition.SUBWOOFER,
            SpeakerPosition.REAR_LEFT,
        ),
    ),
    ;

    fun has(position: SpeakerPosition): Boolean = position in walkOrder
    val hasRear: Boolean get() = walkOrder.any { it.isRear }
    val hasSub: Boolean get() = SpeakerPosition.SUBWOOFER in walkOrder
    val hasDash: Boolean get() = SpeakerPosition.DASH_LEFT in walkOrder

    companion object {
        val DEFAULT = FOUR
        fun fromName(name: String?): SpeakerLayout = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * Output channel bits. The values are identical to android.media.AudioFormat
 * CHANNEL_OUT_*; they are repeated here so this file stays JVM-testable.
 * Android interleaves samples in ascending bit order of the channel mask.
 */
object SpeakerChannels {
    const val FRONT_LEFT = 0x4
    const val FRONT_RIGHT = 0x8
    const val FRONT_CENTER = 0x10
    const val LOW_FREQUENCY = 0x20
    const val BACK_LEFT = 0x40
    const val BACK_RIGHT = 0x80

    /** = AudioFormat.CHANNEL_OUT_STEREO (12). */
    const val STEREO = FRONT_LEFT or FRONT_RIGHT

    /** = AudioFormat.CHANNEL_OUT_QUAD (204). */
    const val QUAD = STEREO or BACK_LEFT or BACK_RIGHT

    /** = AudioFormat.CHANNEL_OUT_5POINT1 (252). */
    const val FIVE_POINT_ONE = QUAD or FRONT_CENTER or LOW_FREQUENCY

    /** Position of [bit] in the interleaved frame for [mask], or -1 when the mask lacks it. */
    fun indexOf(mask: Int, bit: Int): Int =
        if ((mask and bit) == 0) -1 else Integer.bitCount(mask and (bit - 1))

    fun count(mask: Int): Int = Integer.bitCount(mask)
}

/** How the test reaches the speakers. */
enum class SpeakerOutput(val channelMask: Int) {
    /** Two channels; front/rear is the head unit's fader. The common case. */
    STEREO(SpeakerChannels.STEREO),

    /** Four discrete channels: front L/R, rear L/R. */
    QUAD(SpeakerChannels.QUAD),

    /** 5.1: front L/R, centre, LFE (subwoofer), rear L/R. */
    FIVE_POINT_ONE(SpeakerChannels.FIVE_POINT_ONE),
    ;

    val channels: Int get() = SpeakerChannels.count(channelMask)
    val isDirect: Boolean get() = this != STEREO
}

/** The detected output plus what the device said, for the one-line status. */
data class SpeakerOutputInfo(val output: SpeakerOutput, val reportedChannels: Int) {
    val summary: String
        get() = if (output.isDirect) {
            "Direct speaker output (${output.channels} channels)"
        } else {
            "Stereo unit — use the fader for front/rear"
        }
}

/**
 * Highest channel count an output device reports, from its channel counts
 * and positional channel masks. 0 means the device did not say.
 */
fun reportedChannelCount(channelCounts: IntArray, channelMasks: IntArray): Int {
    val fromCounts = channelCounts.maxOrNull() ?: 0
    val fromMasks = channelMasks.maxOfOrNull { Integer.bitCount(it) } ?: 0
    return maxOf(fromCounts, fromMasks, 0)
}

/**
 * Multichannel only when the device reports enough channels AND a track in
 * that layout actually opens. A track opening alone proves nothing: the mixer
 * accepts quad/5.1 everywhere and quietly folds it down to stereo.
 */
fun chooseSpeakerOutput(reportedChannels: Int, opens: (SpeakerOutput) -> Boolean): SpeakerOutput = when {
    reportedChannels >= 6 && opens(SpeakerOutput.FIVE_POINT_ONE) -> SpeakerOutput.FIVE_POINT_ONE
    reportedChannels >= 4 && opens(SpeakerOutput.QUAD) -> SpeakerOutput.QUAD
    else -> SpeakerOutput.STEREO
}

/** One speaker's tone: which interleaved channels carry it, and what it sounds like. */
data class SpeakerRoute(
    val position: SpeakerPosition,
    val output: SpeakerOutput,
    /** Interleaved channel indices that carry the tone; all others stay silent. */
    val channels: Set<Int>,
    val frequencyHz: Double,
    /** Peak level, 0..1 of full scale. Kept moderate. */
    val amplitude: Double,
    /** Stereo unit and a rear speaker: only audible after moving the unit's fader to the rear. */
    val needsFader: Boolean,
)

const val DOOR_TONE_HZ = 440.0
const val TWEETER_TONE_HZ = 4_000.0
const val SUB_TONE_HZ = 50.0

/** Maps a speaker to the output's channels. Pure, so the routing is unit-tested. */
fun speakerRoute(position: SpeakerPosition, output: SpeakerOutput): SpeakerRoute {
    val mask = output.channelMask
    fun idx(bit: Int) = SpeakerChannels.indexOf(mask, bit)
    val fl = idx(SpeakerChannels.FRONT_LEFT)
    val fr = idx(SpeakerChannels.FRONT_RIGHT)
    return when (position) {
        SpeakerPosition.FRONT_LEFT -> SpeakerRoute(position, output, setOf(fl), DOOR_TONE_HZ, 0.35, needsFader = false)
        SpeakerPosition.FRONT_RIGHT -> SpeakerRoute(position, output, setOf(fr), DOOR_TONE_HZ, 0.35, needsFader = false)
        // Dash tweeters share the front channel (passive crossover); a high
        // tone makes the tweeter the speaker you hear.
        SpeakerPosition.DASH_LEFT -> SpeakerRoute(position, output, setOf(fl), TWEETER_TONE_HZ, 0.15, needsFader = false)
        SpeakerPosition.DASH_RIGHT -> SpeakerRoute(position, output, setOf(fr), TWEETER_TONE_HZ, 0.15, needsFader = false)
        SpeakerPosition.REAR_LEFT -> {
            val bl = idx(SpeakerChannels.BACK_LEFT)
            if (bl >= 0) SpeakerRoute(position, output, setOf(bl), DOOR_TONE_HZ, 0.35, needsFader = false)
            else SpeakerRoute(position, output, setOf(fl), DOOR_TONE_HZ, 0.35, needsFader = true)
        }
        SpeakerPosition.REAR_RIGHT -> {
            val br = idx(SpeakerChannels.BACK_RIGHT)
            if (br >= 0) SpeakerRoute(position, output, setOf(br), DOOR_TONE_HZ, 0.35, needsFader = false)
            else SpeakerRoute(position, output, setOf(fr), DOOR_TONE_HZ, 0.35, needsFader = true)
        }
        SpeakerPosition.SUBWOOFER -> {
            val lfe = idx(SpeakerChannels.LOW_FREQUENCY)
            // No LFE channel: a low tone on every channel; the unit's sub
            // output is fed from the mix and only reproduces the lows.
            val channels = if (lfe >= 0) setOf(lfe) else (0 until output.channels).toSet()
            SpeakerRoute(position, output, channels, SUB_TONE_HZ, 0.45, needsFader = false)
        }
    }
}

/**
 * Mono sine tone as 16-bit samples, with a short fade-in and fade-out so
 * the speakers never click.
 */
fun toneSamples(
    frequencyHz: Double,
    seconds: Double,
    sampleRate: Int,
    amplitude: Double,
    fadeSeconds: Double = 0.03,
): ShortArray {
    val frames = (sampleRate * seconds).toInt().coerceAtLeast(0)
    val peak = amplitude.coerceIn(0.0, 1.0) * Short.MAX_VALUE
    val out = ShortArray(frames)
    val step = 2 * PI * frequencyHz / sampleRate
    for (i in 0 until frames) {
        val t = i.toDouble() / sampleRate
        val edge = minOf(t, seconds - t)
        val env = if (fadeSeconds > 0) (edge / fadeSeconds).coerceIn(0.0, 1.0) else 1.0
        out[i] = (sin(step * i) * peak * env).toInt().toShort()
    }
    return out
}

/**
 * Spreads a mono signal into an interleaved frame of [channels] channels,
 * copying it into [active] and leaving every other channel at zero.
 */
fun interleave(mono: ShortArray, channels: Int, active: Set<Int>): ShortArray {
    require(channels > 0) { "channels must be positive" }
    require(active.all { it in 0 until channels }) { "active channel out of range" }
    val out = ShortArray(mono.size * channels)
    val targets = active.toIntArray()
    for (i in mono.indices) {
        val base = i * channels
        val s = mono[i]
        for (c in targets) out[base + c] = s
    }
    return out
}

/** Interleaved PCM for one speaker's tone in the route's output layout. */
fun renderRoute(route: SpeakerRoute, seconds: Double, sampleRate: Int): ShortArray =
    interleave(
        toneSamples(route.frequencyHz, seconds, sampleRate, route.amplitude),
        route.output.channels,
        route.channels,
    )
