package com.autoverse.launcher.core.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the live map measured on this device; shown in Diagnostics. */
data class MapRenderSnapshot(
    /** Frames per second while the map was actually moving (idle time excluded). */
    val activeFps: Double = 0.0,
    /** Frames that took longer than [SLOW_FRAME_MS] in the last window. */
    val slowFrames: Int = 0,
    /** Longest frame interval seen in the last window, in ms. */
    val worstFrameMs: Double = 0.0,
    /** Frames rendered since the map started. */
    val totalFrames: Long = 0,
) {
    /** The pass bar for the head-unit test: smooth and no visible hitches. */
    val meetsTarget: Boolean
        get() = totalFrames > 0 && activeFps >= TARGET_FPS && worstFrameMs <= SLOW_FRAME_MS
}

const val SLOW_FRAME_MS = 50.0
const val TARGET_FPS = 25.0

/** Gaps longer than this are idle time (nothing changed), not slow frames. */
const val IDLE_GAP_MS = 500.0

/** How often a snapshot is published; the readout never drives extra recomposition. */
const val PUBLISH_WINDOW_MS = 1_000L

/**
 * Pure accumulator for map frame timing. MapLibre only renders when
 * something changes, so the interval between two finished frames is either a
 * real frame time (while panning or following) or idle time; only the former
 * counts towards fps and slow frames.
 */
class MapFrameAccumulator {
    private var lastFrameMs = -1L
    private var windowStartMs = -1L
    private var activeMs = 0.0
    private var activeFrames = 0
    private var slow = 0
    private var worst = 0.0
    private var total = 0L

    /** Records a finished frame at [nowMs]; returns a snapshot when a window closes. */
    fun onFrame(nowMs: Long): MapRenderSnapshot? {
        total++
        if (windowStartMs < 0) windowStartMs = nowMs
        if (lastFrameMs >= 0) {
            val interval = (nowMs - lastFrameMs).toDouble()
            if (interval <= IDLE_GAP_MS) {
                activeMs += interval
                activeFrames++
                if (interval > SLOW_FRAME_MS) slow++
                if (interval > worst) worst = interval
            }
        }
        lastFrameMs = nowMs
        if (nowMs - windowStartMs < PUBLISH_WINDOW_MS) return null
        val snapshot = MapRenderSnapshot(
            activeFps = if (activeMs > 0) activeFrames * 1000.0 / activeMs else 0.0,
            slowFrames = slow,
            worstFrameMs = worst,
            totalFrames = total,
        )
        windowStartMs = nowMs
        activeMs = 0.0
        activeFrames = 0
        slow = 0
        worst = 0.0
        return snapshot
    }
}

/** App-wide holder so Diagnostics can show what the map measured. */
object MapRenderStats {
    private val _snapshot = MutableStateFlow(MapRenderSnapshot())
    val snapshot: StateFlow<MapRenderSnapshot> = _snapshot.asStateFlow()

    internal fun publish(snapshot: MapRenderSnapshot) {
        _snapshot.value = snapshot
    }
}
