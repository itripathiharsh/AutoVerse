package com.autoverse.launcher.feature.map

/** Why the embedded-map prototype cannot serve map data. */
enum class MapPocFailure {
    STYLE_LOAD_FAILED,
    LOAD_TIMEOUT,
    RUNTIME_FAILURE,
}

/**
 * Phase 7 prototype states. Every reachable outcome has a visible
 * representation so the screen never shows a silent blank surface, and the
 * navigation hand-off fallback is recommended exactly when the map cannot
 * render. None of this state touches the production dashboard.
 */
sealed interface MapPocState {
    /** True when the map cannot render and the hand-off is the fallback. */
    val showsFallback: Boolean

    data object Loading : MapPocState {
        override val showsFallback: Boolean = false
    }

    data object Ready : MapPocState {
        override val showsFallback: Boolean = false
    }

    /** The style loaded but declares no sources; there is nothing to draw. */
    data object Empty : MapPocState {
        override val showsFallback: Boolean = true
    }

    data class Error(val failure: MapPocFailure) : MapPocState {
        override val showsFallback: Boolean = true
    }

    /** The renderer itself could not start; terminal for this screen visit. */
    data class Unavailable(val reason: String) : MapPocState {
        override val showsFallback: Boolean = true
    }
}

fun MapPocFailure.message(): String = when (this) {
    MapPocFailure.STYLE_LOAD_FAILED ->
        "The map style couldn't be loaded. Check the network connection and try again."
    MapPocFailure.LOAD_TIMEOUT ->
        "The map took too long to load. Check the network connection and try again."
    MapPocFailure.RUNTIME_FAILURE ->
        "The map stopped rendering. Try reloading it."
}
