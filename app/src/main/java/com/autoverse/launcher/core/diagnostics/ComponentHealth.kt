package com.autoverse.launcher.core.diagnostics

import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.media.MediaUiState

/**
 * Lifecycle/health of one launcher component as reported on the diagnostics
 * screen. [DISABLED] is a deliberate off state (e.g. the experimental map),
 * distinct from [UNAVAILABLE] (nothing can serve the component right now) and
 * [ERROR] (it failed). [NOT_CHECKED] covers states that are still resolving.
 */
enum class ComponentStatus {
    READY,
    LOADING,
    PERMISSION_REQUIRED,
    DISABLED,
    UNAVAILABLE,
    ERROR,
    NOT_CHECKED,
}

/** One component tile: real status plus a factual one-line explanation. */
data class ComponentHealth(
    val name: String,
    val status: ComponentStatus,
    val detail: String,
)

/**
 * The component builders below map already-observed state onto tiles; they
 * run no checks of their own, so the screen and this mapping can never
 * disagree with what the rest of the app shows. Pure and unit-tested.
 */
fun navigationHandoffHealth(
    providerLabel: String,
    preferredMissing: Boolean,
    hasLaunchTarget: Boolean,
): ComponentHealth = if (!hasLaunchTarget) {
    ComponentHealth(
        name = "Navigation handoff",
        status = ComponentStatus.UNAVAILABLE,
        detail = "No app can receive navigation hand-offs right now.",
    )
} else {
    ComponentHealth(
        name = "Navigation handoff",
        status = ComponentStatus.READY,
        detail = if (preferredMissing) {
            "The preferred app isn't installed; $providerLabel is used instead."
        } else {
            "$providerLabel is used for hand-offs."
        },
    )
}

fun mediaSessionHealth(
    notificationAccessGranted: Boolean,
    state: MediaUiState,
): ComponentHealth {
    val status = when {
        !notificationAccessGranted -> ComponentStatus.PERMISSION_REQUIRED
        state is MediaUiState.AccessRequired -> ComponentStatus.PERMISSION_REQUIRED
        state is MediaUiState.Connecting -> ComponentStatus.LOADING
        state is MediaUiState.Error -> ComponentStatus.ERROR
        else -> ComponentStatus.READY
    }
    val detail = when {
        !notificationAccessGranted || state is MediaUiState.AccessRequired ->
            "Grant notification access so the media card can read sessions."
        state is MediaUiState.Connecting -> "Reading media session state…"
        state is MediaUiState.Error -> state.message
        state is MediaUiState.Active ->
            "Connected to ${state.appLabel ?: "a media player"}."
        else -> "Access granted; no player is active right now."
    }
    return ComponentHealth(name = "Media card", status = status, detail = detail)
}

fun appDrawerHealth(state: AppListState): ComponentHealth = when (state) {
    is AppListState.Loading -> ComponentHealth(
        name = "App drawer",
        status = ComponentStatus.LOADING,
        detail = "Reading installed apps…",
    )

    is AppListState.Ready -> ComponentHealth(
        name = "App drawer",
        status = ComponentStatus.READY,
        detail = "${state.apps.size} apps available.",
    )

    is AppListState.Error -> ComponentHealth(
        name = "App drawer",
        status = ComponentStatus.ERROR,
        detail = state.message,
    )
}

fun settingsStoreHealth(readable: Boolean?): ComponentHealth = when (readable) {
    null -> ComponentHealth(
        name = "Settings storage",
        status = ComponentStatus.NOT_CHECKED,
        detail = "Checked during startup.",
    )

    true -> ComponentHealth(
        name = "Settings storage",
        status = ComponentStatus.READY,
        detail = "Preferences loaded from disk.",
    )

    false -> ComponentHealth(
        name = "Settings storage",
        status = ComponentStatus.ERROR,
        detail = "Settings file unreadable; defaults are in use.",
    )
}

fun frameTrackingHealth(tracking: Boolean): ComponentHealth = if (tracking) {
    ComponentHealth(
        name = "Frame sampling",
        status = ComponentStatus.READY,
        detail = "Sampling this screen while it is visible.",
    )
} else {
    ComponentHealth(
        name = "Frame sampling",
        status = ComponentStatus.DISABLED,
        detail = "Paused; values keep their last reading.",
    )
}

