package com.autoverse.launcher.core.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Phone-call surface state; never fabricated — Idle means "nothing observed". */
sealed interface CallUiState {
    data object Idle : CallUiState

    /** The platform reported CALL_STATE_RINGING. [number] is what the radio supplied. */
    data class Incoming(val number: String?) : CallUiState

    /** The platform reported CALL_STATE_OFFHOOK. */
    data class InCall(val number: String?) : CallUiState
}

/** Pure decision: the Home side panel shows the call card only when justified. */
fun showsIncomingCall(state: CallUiState, permissionGranted: Boolean): Boolean =
    permissionGranted && state !is CallUiState.Idle

/** The number is shown only when the radio actually supplied one. */
fun callDisplayNumber(number: String?): String? =
    number?.trim()?.takeIf { it.isNotEmpty() }

/**
 * Callback-driven call-state reader (TelephonyManager). Least-privileged:
 * READ_PHONE_STATE only — no call-log, no contacts, no recording, and no
 * polling; the platform pushes every change. Observing runs only while the
 * caller keeps it started (Home), so backgrounded listening never happens.
 *
 * The caller's number is only available where the platform provides it
 * (legacy listener path); on newer APIs the card shows a factual
 * "Incoming call" without a number rather than inventing one.
 */
class CallStateMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val telephonyManager =
        appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val _callState = MutableStateFlow<CallUiState>(CallUiState.Idle)
    val callState: StateFlow<CallUiState> = _callState.asStateFlow()

    val hasReadPhoneStatePermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED

    private var listening = false

    private val legacyListener = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                _callState.value = stateOf(state, phoneNumber)
            }
        }
    } else {
        null
    }

    private val modernCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                _callState.value = stateOf(state, phoneNumber = null)
            }
        }
    } else {
        null
    }

    /** Begins platform call-state callbacks; safe to call repeatedly. */
    fun start() {
        val manager = telephonyManager ?: return
        if (listening || !hasReadPhoneStatePermission) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && modernCallback != null) {
                manager.registerTelephonyCallback(
                    ContextCompat.getMainExecutor(appContext),
                    modernCallback,
                )
            } else if (legacyListener != null) {
                @Suppress("DEPRECATION")
                manager.listen(legacyListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
            listening = true
        } catch (_: Exception) {
            listening = false
        }
    }

    /** Stops platform callbacks and resets to Idle; safe to call repeatedly. */
    fun stop() {
        if (!listening) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && modernCallback != null) {
                telephonyManager?.unregisterTelephonyCallback(modernCallback)
            } else if (legacyListener != null) {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(legacyListener, PhoneStateListener.LISTEN_NONE)
            }
        } catch (_: Exception) {
            // The flow resets below regardless; nothing may outlive the monitor.
        }
        listening = false
        _callState.value = CallUiState.Idle
    }

    private fun stateOf(state: Int, phoneNumber: String?): CallUiState = when (state) {
        TelephonyManager.CALL_STATE_RINGING -> CallUiState.Incoming(phoneNumber)
        TelephonyManager.CALL_STATE_OFFHOOK -> CallUiState.InCall(phoneNumber)
        else -> CallUiState.Idle
    }
}
