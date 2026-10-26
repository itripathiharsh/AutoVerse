package com.autoverse.launcher.core.design.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.autoverse.launcher.core.navigation.MapsForegroundVerdict
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.foregroundVerdict
import com.autoverse.launcher.core.navigation.navigationFailureMessage
import com.autoverse.launcher.core.navigation.stayedInFrontMessage
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** How long a requested launch has to cover AutoVerse before we say it didn't. */
private const val FOREGROUND_TIMEOUT_MS = 3_000L

/**
 * Runs a navigation launch and reports only what actually happened: a
 * failure outcome is explained immediately; a [NavigationOutcome.REQUESTED]
 * launch is followed by a short watch of AutoVerse's own lifecycle — being
 * paused means another activity took the screen, staying resumed means the
 * request went nowhere and the user is told so instead of seeing silence.
 */
class NavigationLaunchFeedback internal constructor(
    private val scope: CoroutineScope,
    private val snackbar: SnackbarHostState,
    private val lifecycle: () -> Lifecycle,
    private val inMultiWindow: () -> Boolean,
    private val onVerdict: (String, MapsForegroundVerdict) -> Unit,
) {
    fun run(targetLabel: String, launch: suspend () -> NavigationOutcome) {
        scope.launch {
            val outcome = launch()
            if (outcome != NavigationOutcome.REQUESTED) {
                snackbar.showSnackbar(
                    message = navigationFailureMessage(targetLabel, outcome),
                    withDismissAction = true,
                )
                return@launch
            }
            val paused = awaitPause(lifecycle(), FOREGROUND_TIMEOUT_MS)
            val verdict = foregroundVerdict(pausedAfterRequest = paused, inMultiWindow = inMultiWindow())
            onVerdict(targetLabel, verdict)
            if (verdict == MapsForegroundVerdict.STAYED_ON_AUTOVERSE) {
                snackbar.showSnackbar(message = stayedInFrontMessage(targetLabel), withDismissAction = true)
            }
        }
    }
}

@Composable
fun rememberNavigationLaunchFeedback(
    snackbar: SnackbarHostState,
    onVerdict: (String, MapsForegroundVerdict) -> Unit = { _, _ -> },
): NavigationLaunchFeedback {
    val scope = rememberCoroutineScope()
    val lifecycleOwner by rememberUpdatedState(LocalLifecycleOwner.current)
    val context = LocalContext.current
    val latestOnVerdict by rememberUpdatedState(onVerdict)
    return remember(scope, snackbar, context) {
        NavigationLaunchFeedback(
            scope = scope,
            snackbar = snackbar,
            lifecycle = { lifecycleOwner.lifecycle },
            inMultiWindow = {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                    context.findActivity()?.isInMultiWindowMode == true
            },
            onVerdict = { label, verdict -> latestOnVerdict(label, verdict) },
        )
    }
}

/** True when [lifecycle] leaves RESUMED within [timeoutMs]. Main-thread only. */
private suspend fun awaitPause(lifecycle: Lifecycle, timeoutMs: Long): Boolean =
    withTimeoutOrNull(timeoutMs) {
        suspendCancellableCoroutine { continuation ->
            if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: androidx.lifecycle.LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_PAUSE && continuation.isActive) {
                        lifecycle.removeObserver(this)
                        continuation.resume(Unit)
                    }
                }
            }
            lifecycle.addObserver(observer)
            continuation.invokeOnCancellation { lifecycle.removeObserver(observer) }
        }
    } != null

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
