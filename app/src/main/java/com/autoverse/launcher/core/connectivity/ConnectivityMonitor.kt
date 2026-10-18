package com.autoverse.launcher.core.connectivity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Tri-state link readability: the UI hides indicators it cannot verify. */
enum class LinkState { CONNECTED, DISCONNECTED, UNKNOWN }

/** Wi-Fi and Bluetooth availability for the dashboard status area. */
data class ConnectivityStatus(
    val wifi: LinkState = LinkState.UNKNOWN,
    val bluetooth: LinkState = LinkState.UNKNOWN,
)

/**
 * Bluetooth state is only readable below API 31 (normal BLUETOOTH
 * permission, declared maxSdkVersion 30) or when BLUETOOTH_CONNECT was
 * already granted — the launcher never prompts for it, so modern devices
 * simply report UNKNOWN and the indicator stays hidden. Pure for JVM tests.
 */
fun isBluetoothReadable(sdkInt: Int, hasConnectPermission: Boolean): Boolean =
    sdkInt < Build.VERSION_CODES.S || hasConnectPermission

/**
 * Lifecycle-driven connectivity watcher. Registers the default-network
 * callback and the Bluetooth state receiver only while this flow is
 * collected; both registrations are torn down in [awaitClose]. Emits an
 * initial snapshot so collectors never wait for the first change. The
 * network callback requires API 24, so on API 23 the flow still emits the
 * initial snapshot but does not observe later network changes.
 */
class ConnectivityMonitor(context: Context) {

    private val appContext = context.applicationContext

    fun observe(): Flow<ConnectivityStatus> = callbackFlow {
        val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(readStatus())
            }

            override fun onLost(network: Network) {
                trySend(readStatus())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(readStatus())
            }
        }
        if (connectivityManager != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        ) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
        trySend(readStatus())

        val bluetoothReceiver =
            if (isBluetoothReadable(Build.VERSION.SDK_INT, hasBluetoothConnectPermission())) {
                object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                            trySend(readStatus())
                        }
                    }
                }.also { receiver ->
                    ContextCompat.registerReceiver(
                        appContext,
                        receiver,
                        IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                }
            } else {
                null
            }

        awaitClose {
            if (connectivityManager != null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
            ) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
            if (bluetoothReceiver != null) {
                appContext.unregisterReceiver(bluetoothReceiver)
            }
        }
    }

    private fun readStatus(): ConnectivityStatus = ConnectivityStatus(
        wifi = readWifiState(),
        bluetooth = readBluetoothState(),
    )

    private fun readWifiState(): LinkState {
        val connectivityManager =
            appContext.getSystemService(ConnectivityManager::class.java) ?: return LinkState.UNKNOWN
        val capabilities = try {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        } catch (_: SecurityException) {
            null
        } ?: return LinkState.DISCONNECTED
        return if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            LinkState.CONNECTED
        } else {
            LinkState.DISCONNECTED
        }
    }

    private fun readBluetoothState(): LinkState {
        if (!isBluetoothReadable(Build.VERSION.SDK_INT, hasBluetoothConnectPermission())) {
            return LinkState.UNKNOWN
        }
        val adapter = try {
            appContext.getSystemService(BluetoothManager::class.java)?.adapter
        } catch (_: SecurityException) {
            null
        } ?: return LinkState.UNKNOWN
        return try {
            if (adapter.isEnabled) LinkState.CONNECTED else LinkState.DISCONNECTED
        } catch (_: SecurityException) {
            LinkState.UNKNOWN
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
}
