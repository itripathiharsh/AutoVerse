package com.autoverse.launcher.feature.map

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phase 7 — pure state machine for the embedded-map prototype. MapLibre
 * callbacks and the load-timeout guard feed events in; the screen renders
 * [state]. Free of Android types so every transition is JVM-testable.
 */
class MapPocController {

    private val _state = MutableStateFlow<MapPocState>(MapPocState.Loading)
    val state: StateFlow<MapPocState> = _state.asStateFlow()

    /**
     * The style finished parsing. A late success is accepted even after a
     * timeout error, because the map really did load.
     */
    fun onStyleLoaded(sourceCount: Int) {
        if (isUnavailable()) return
        _state.value = if (sourceCount > 0) MapPocState.Ready else MapPocState.Empty
    }

    /** Style failures are only meaningful while a load is in flight. */
    fun onStyleLoadFailed(failure: MapPocFailure = MapPocFailure.STYLE_LOAD_FAILED) {
        if (_state.value !is MapPocState.Loading) return
        _state.value = MapPocState.Error(failure)
    }

    /** A live map stopped rendering (renderer failure after first load). */
    fun onRuntimeFailure() {
        if (isUnavailable() || _state.value is MapPocState.Error) return
        _state.value = MapPocState.Error(MapPocFailure.RUNTIME_FAILURE)
    }

    /** The MapView itself could not start; terminal for this screen visit. */
    fun onSurfaceUnavailable(reason: String) {
        _state.value = MapPocState.Unavailable(reason)
    }

    /** Retry from Error or Empty; ignored while Loading, Ready or Unavailable. */
    fun reload() {
        when (_state.value) {
            is MapPocState.Error, MapPocState.Empty -> _state.value = MapPocState.Loading
            else -> Unit
        }
    }

    private fun isUnavailable(): Boolean = _state.value is MapPocState.Unavailable
}
