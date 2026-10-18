package com.autoverse.launcher.core.performance

import android.view.Choreographer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Choreographer-driven sampler for the diagnostics readout. Phase 9: it is
 * started only while the visible screen actually shows frame data and the
 * activity is resumed (see AppRoot), so everything it publishes describes
 * visible-screen rendering only. Phase 17: every [start] begins a new session
 * ([Snapshot.sessionId]) and each published window carries a wall-clock
 * [Snapshot.atMillis], so diagnostics can chart a bounded sample history and
 * discard stale samples across restarts. [clock] is injectable for tests.
 */
class FrameTimeTracker(
    private val clock: () -> Long = System::currentTimeMillis,
) {

    data class Snapshot(
        val fps: Float = 0f,
        val frameCount: Long = 0L,
        val jankCount: Long = 0L,
        val sessionId: Long = 0L,
        val atMillis: Long = 0L,
    )

    private val accumulator = FrameStatsAccumulator()

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot

    private val _running = MutableStateFlow(false)

    /** True while the tracker is actively sampling; drives diagnostics copy. */
    val running: StateFlow<Boolean> = _running

    private val choreographer: Choreographer = Choreographer.getInstance()
    private var active = false
    private var sessionId = 0L

    private val callback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            accumulator.record(frameTimeNanos)?.let { fps ->
                _snapshot.value = Snapshot(
                    fps = fps,
                    frameCount = accumulator.frameCount,
                    jankCount = accumulator.jankCount,
                    sessionId = sessionId,
                    atMillis = clock(),
                )
            }
            if (active) {
                choreographer.postFrameCallback(this)
            }
        }
    }

    fun start() {
        if (active) return
        active = true
        sessionId += 1
        accumulator.resetWindow()
        _running.value = true
        choreographer.postFrameCallback(callback)
    }

    fun stop() {
        if (!active) return
        active = false
        _running.value = false
        choreographer.removeFrameCallback(callback)
    }
}

/**
 * Pure frame math behind [FrameTimeTracker]: one-second windows of FPS plus
 * cumulative frame and jank counters. Android-free so the state handling is
 * unit-tested (FrameStatsAccumulatorTest). A frame counts as jank when the
 * gap to the previous frame exceeds 33 ms (two 60 Hz frame deadlines).
 */
class FrameStatsAccumulator {

    var frameCount: Long = 0L
        private set

    var jankCount: Long = 0L
        private set

    private var primed = false
    private var lastFrameNanos = 0L
    private var windowStartNanos = 0L
    private var framesThisWindow = 0

    /** Drops in-flight window state but keeps the cumulative counters. */
    fun resetWindow() {
        primed = false
        lastFrameNanos = 0L
        windowStartNanos = 0L
        framesThisWindow = 0
    }

    /**
     * Records one frame. Returns the window FPS when a one-second window
     * closes, or null while the window is still filling.
     */
    fun record(frameTimeNanos: Long): Float? {
        if (!primed) {
            primed = true
            lastFrameNanos = frameTimeNanos
            windowStartNanos = frameTimeNanos
            return null
        }
        val frameDelta = frameTimeNanos - lastFrameNanos
        lastFrameNanos = frameTimeNanos
        framesThisWindow++
        frameCount++
        if (frameDelta > JANK_THRESHOLD_NANOS) {
            jankCount++
        }

        val windowElapsed = frameTimeNanos - windowStartNanos
        if (windowElapsed < ONE_SECOND_NANOS) return null
        val fps = framesThisWindow * 1_000_000_000f / windowElapsed
        framesThisWindow = 0
        windowStartNanos = frameTimeNanos
        return fps
    }

    private companion object {
        const val ONE_SECOND_NANOS = 1_000_000_000L
        const val JANK_THRESHOLD_NANOS = 33_000_000L
    }
}
