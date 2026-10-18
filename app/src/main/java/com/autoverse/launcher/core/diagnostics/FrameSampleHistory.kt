package com.autoverse.launcher.core.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One published one-second FPS window. */
data class FrameSample(
    val fps: Float,
    val atMillis: Long,
)

/**
 * Phase 17 — bounded in-memory history of [FrameSample]s for the diagnostics
 * sparkline. Samples arrive once per closed one-second window while the frame
 * tracker runs, so the history describes visible-screen rendering only and
 * stops growing the moment sampling stops. Oldest samples fall off past
 * [limit]; the history lives for the tracker session and is cleared when a
 * new session starts.
 */
class FrameSampleHistory(private val limit: Int = DEFAULT_LIMIT) {

    private val _samples = MutableStateFlow<List<FrameSample>>(emptyList())

    /** Samples oldest-first, at most [limit] entries. */
    val samples: StateFlow<List<FrameSample>> = _samples.asStateFlow()

    fun add(fps: Float, atMillis: Long) {
        _samples.value = (_samples.value + FrameSample(fps = fps, atMillis = atMillis)).takeLast(limit)
    }

    fun clear() {
        _samples.value = emptyList()
    }

    companion object {
        const val DEFAULT_LIMIT = 60
    }
}
