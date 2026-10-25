package com.autoverse.launcher.core.bridge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.ui.graphics.asAndroidBitmap
import com.autoverse.launcher.MainViewModel
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.connectivity.LinkState
import com.autoverse.launcher.core.launcher.AppListState
import com.autoverse.launcher.core.media.MediaCommand
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.isNotificationAccessEnabled
import com.autoverse.launcher.core.media.isPlaying
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.model.InstalledApp
import com.autoverse.launcher.core.model.PerformanceProfile
import com.autoverse.launcher.core.navigation.DestinationParseResult
import com.autoverse.launcher.core.obd.ObdConnectionState
import com.autoverse.launcher.core.voice.OfflineVoiceState
import com.autoverse.launcher.core.navigation.parseNavigationDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Bi-directional JavaScript Bridge between the AutoVerse Web Cockpit and the
 * Android automotive launcher backend services.
 *
 * Runs locally inside a hardware-accelerated WebView with zero cloud overhead,
 * zero paid APIs, and instant 60 FPS event handling.
 */
class AutoVerseBridge(
    private val context: Context,
    private val viewModel: MainViewModel,
    private val webView: WebView,
    private val scope: CoroutineScope
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Set by CockpitScreen: launches the platform speech recognizer for the
     * requested mode ("command" | "apps" | "navigation") on the main thread.
     * The result comes back through [pushVoiceResult].
     */
    var voiceRequestHandler: ((String) -> Unit)? = null

    /**
     * Set by CockpitScreen: launches the BLUETOOTH_CONNECT runtime prompt
     * (API 31+) on the main thread. The result comes back through
     * [pushObdPermissionResult].
     */
    var obdPermissionHandler: (() -> Unit)? = null

    /**
     * Set by CockpitScreen: launches the RECORD_AUDIO runtime prompt on the
     * main thread. The result comes back through [pushMicPermissionResult].
     */
    var micPermissionHandler: (() -> Unit)? = null

    @JavascriptInterface
    fun startVoiceRecognition(mode: String) {
        mainHandler.post {
            voiceRequestHandler?.invoke(mode)
        }
    }

    @JavascriptInterface
    fun connectObd() {
        mainHandler.post { viewModel.connectObd() }
    }

    @JavascriptInterface
    fun disconnectObd() {
        mainHandler.post { viewModel.disconnectObd() }
    }

    @JavascriptInterface
    fun requestObdBluetoothPermission() {
        mainHandler.post { obdPermissionHandler?.invoke() }
    }

    @JavascriptInterface
    fun isOfflineVoiceEnabled(): Boolean = viewModel.settings.value.offlineVoiceEnabled

    /** Current effective workload profile ("CONSTRAINED" | "BALANCED" | "ENHANCED"). */
    @JavascriptInterface
    fun isThemeSoundsEnabled(): Boolean = viewModel.settings.value.themeSoundsEnabled

    @JavascriptInterface
    fun setThemeSoundsEnabled(enabled: Boolean) {
        mainHandler.post { viewModel.setThemeSoundsEnabled(enabled) }
    }

    @JavascriptInterface
    fun getWorkloadProfile(): String = viewModel.performanceSpec.value.profile.name

    /** Live profile updates so the cockpit can re-tier its effects instantly. */
    fun pushWorkloadProfile(name: String) {
        postJs(
            "window.AutoVerse && window.AutoVerse.updateWorkloadProfile(" +
                JSONObject().put("profile", name) + ");",
        )
    }

    @JavascriptInterface
    fun setOfflineVoiceEnabled(enabled: Boolean) {
        mainHandler.post { viewModel.setOfflineVoiceEnabled(enabled) }
    }

    @JavascriptInterface
    fun requestMicPermission() {
        mainHandler.post { micPermissionHandler?.invoke() }
    }

    /** Cockpit-side callback: the mic prompt finished; grant enables the feature. */
    fun pushMicPermissionResult(granted: Boolean) {
        mainHandler.post { viewModel.onMicPermissionResult(granted) }
    }

    /** Streams download/listen/capture states to the cockpit orb. */
    fun pushOfflineVoiceState(state: OfflineVoiceState) {
        scope.launch(Dispatchers.Default) {
            val json = JSONObject()
            when (state) {
                is OfflineVoiceState.Downloading -> {
                    json.put("state", "downloading")
                    json.put("percent", state.percent)
                }
                OfflineVoiceState.Preparing -> json.put("state", "preparing")
                OfflineVoiceState.ListeningForWake -> json.put("state", "wake")
                OfflineVoiceState.CapturingCommand -> json.put("state", "capture")
                is OfflineVoiceState.Error -> {
                    json.put("state", "error")
                    json.put("error", state.message)
                }
                OfflineVoiceState.Disabled -> json.put("state", "disabled")
            }
            postJs("window.AutoVerse && window.AutoVerse.updateOfflineVoiceState($json);")
        }
    }

    /** Cockpit-side callback: the runtime permission prompt finished. */
    fun pushObdPermissionResult(granted: Boolean) {
        mainHandler.post {
            if (granted) {
                viewModel.connectObd()
            } else {
                val json = JSONObject().put("state", "error").put("error", "Bluetooth permission was denied")
                postJs("window.AutoVerse && window.AutoVerse.updateObdState($json);")
            }
        }
    }

    /** Streams the live OBD connection state (and sample) into the cockpit. */
    fun pushObdState(state: ObdConnectionState) {
        scope.launch(Dispatchers.Default) {
            val json = JSONObject()
            when (state) {
                is ObdConnectionState.Connected -> {
                    json.put("state", "connected")
                    json.put("deviceName", state.deviceName)
                    state.sample.rpm?.let { json.put("rpm", it) }
                    state.sample.speedKmh?.let { json.put("speedKmh", it) }
                    state.sample.coolantC?.let { json.put("coolantC", it) }
                    state.sample.fuelPct?.let { json.put("fuelPct", it) }
                    state.sample.loadPct?.let { json.put("loadPct", it) }
                    state.sample.throttlePct?.let { json.put("throttlePct", it) }
                }
                is ObdConnectionState.Connecting -> {
                    json.put("state", "connecting")
                    json.put("deviceName", state.deviceName)
                }
                is ObdConnectionState.PermissionRequired -> json.put("state", "permission")
                is ObdConnectionState.Error -> {
                    json.put("state", "error")
                    json.put("error", state.message)
                }
                ObdConnectionState.Disconnected -> json.put("state", "disconnected")
            }
            postJs("window.AutoVerse && window.AutoVerse.updateObdState($json);")
        }
    }

    /** Delivers the recognizer outcome (transcribed text or an error) to the cockpit. */
    fun pushVoiceResult(text: String?, mode: String, error: String? = null) {
        val json = JSONObject()
        json.put("ok", text != null)
        if (text != null) json.put("text", text)
        if (error != null) json.put("error", error)
        json.put("mode", mode)
        postJs("window.AutoVerse && window.AutoVerse.onVoiceResult($json);")
    }

    @JavascriptInterface
    fun sendMediaCommand(action: String) {
        scope.launch {
            when (action) {
                "PLAY" -> viewModel.sendMediaCommand(MediaCommand.PLAY)
                "PAUSE" -> viewModel.sendMediaCommand(MediaCommand.PAUSE)
                "PLAY_PAUSE" -> {
                    val active = viewModel.mediaState.value as? MediaUiState.Active
                    val cmd = if (active?.status?.isPlaying == true) MediaCommand.PAUSE else MediaCommand.PLAY
                    viewModel.sendMediaCommand(cmd)
                }
                "NEXT" -> viewModel.sendMediaCommand(MediaCommand.NEXT)
                "PREVIOUS" -> viewModel.sendMediaCommand(MediaCommand.PREVIOUS)
                else -> {
                    if (action.startsWith("SEEK:")) {
                        action.removePrefix("SEEK:").toLongOrNull()?.let { targetMs ->
                            viewModel.seekMediaTo(targetMs)
                        }
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun launchApp(packageName: String, activityName: String) {
        scope.launch {
            val listState = viewModel.appListState.value
            if (listState is AppListState.Ready) {
                val found = listState.apps.firstOrNull {
                    it.packageName == packageName && (activityName.isEmpty() || it.activityName == activityName)
                }
                if (found != null) {
                    viewModel.launchApp(found)
                    return@launch
                }
            }
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            } catch (_: Exception) {}
        }
    }

    @JavascriptInterface
    fun launchAppByName(name: String) {
        scope.launch {
            val lower = name.trim().lowercase()
            when {
                lower.contains("phone") || lower.contains("call") -> {
                    try {
                        val intent = Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return@launch
                    } catch (_: Exception) {}
                }
                lower.contains("map") || lower.contains("nav") -> {
                    viewModel.openNavigationApp()
                    return@launch
                }
            }

            val listState = viewModel.appListState.value
            if (listState is AppListState.Ready) {
                val found = listState.apps.firstOrNull {
                    it.label.contains(name, ignoreCase = true) || it.packageName.contains(name, ignoreCase = true)
                }
                if (found != null) {
                    viewModel.launchApp(found)
                    return@launch
                }
            }
        }
    }

    @JavascriptInterface
    fun openNavigation(query: String?) {
        scope.launch {
            if (query.isNullOrBlank()) {
                viewModel.openNavigationApp()
            } else {
                when (val parsed = parseNavigationDestination(query)) {
                    is DestinationParseResult.Valid -> {
                        viewModel.navigateToDestination(parsed.destination)
                    }
                    else -> {
                        val uri = Uri.parse("google.navigation:q=" + Uri.encode(query))
                        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        try {
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            viewModel.openNavigationApp()
                        }
                    }
                }
            }
        }
    }

    @JavascriptInterface
    fun setProfile(profile: String) {
        val mapped = when (profile.trim().lowercase()) {
            "constrained" -> PerformanceProfile.CONSTRAINED
            "enhanced" -> PerformanceProfile.ENHANCED
            else -> PerformanceProfile.BALANCED
        }
        viewModel.setProfile(mapped)
    }

    @JavascriptInterface
    fun isPremiumUnlocked(): Boolean = viewModel.settings.value.premiumUnlocked

    @JavascriptInterface
    fun setPremiumUnlocked(unlocked: Boolean) {
        viewModel.setPremiumUnlocked(unlocked)
    }

    @JavascriptInterface
    fun setTheme(themeId: String) {
        val mapped = when (themeId.trim().lowercase()) {
            "theme-cobalt", "cobalt" -> AutoVerseThemeId.COBALT
            "theme-obsidian", "obsidian" -> AutoVerseThemeId.OBSIDIAN
            "theme-neon-pulse", "neon-pulse" -> AutoVerseThemeId.NEON_PULSE
            "theme-atelier", "atelier" -> AutoVerseThemeId.ATELIER
            "theme-apex", "apex" -> AutoVerseThemeId.APEX
            "theme-sakura", "sakura" -> AutoVerseThemeId.SAKURA
            "theme-neon-tokyo", "neon-tokyo" -> AutoVerseThemeId.NEON_TOKYO
            "theme-shonen", "shonen" -> AutoVerseThemeId.SHONEN
            "theme-gotham", "gotham" -> AutoVerseThemeId.GOTHAM
            "theme-hot-rod", "hot-rod" -> AutoVerseThemeId.HOT_ROD
            "theme-fortress", "fortress" -> AutoVerseThemeId.FORTRESS
            "theme-web-rush", "web-rush" -> AutoVerseThemeId.WEB_RUSH
            else -> AutoVerseThemeId.COBALT
        }
        viewModel.setTheme(mapped)
    }

    private val iconCache = ConcurrentHashMap<String, String>()

    @JavascriptInterface
    fun openNotificationAccessSettings() {
        mainHandler.post {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    @JavascriptInterface
    fun isNotificationAccessGranted(): Boolean {
        return isNotificationAccessEnabled(context)
    }

    @JavascriptInterface
    fun refreshMediaSession() {
        mainHandler.post {
            viewModel.refreshMediaSession()
            pushMediaState(viewModel.mediaState.value)
        }
    }

    private fun getAppIconBase64(app: InstalledApp): String? {
        val cached = iconCache[app.key]
        if (cached != null) return cached
        return try {
            val pm = context.packageManager
            val drawable: Drawable = try {
                pm.getActivityIcon(app.component)
            } catch (_: Exception) {
                pm.getApplicationIcon(app.packageName)
            }
            val size = 80
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 85, stream)
            val b64 = "data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            iconCache[app.key] = b64
            b64
        } catch (_: Exception) {
            null
        }
    }

    @JavascriptInterface
    fun requestInitialState() {
        viewModel.refreshMediaSession()
        viewModel.ensureAppsLoaded()
        mainHandler.post {
            pushMediaState(viewModel.mediaState.value)
            pushAppListState(viewModel.appListState.value)
            pushConnectivity(viewModel.connectivity.value)
            requestLocation()
        }
    }

    @JavascriptInterface
    fun requestLocation() {
        mainHandler.post {
            try {
                val locManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val isGpsEnabled = locManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
                val isNetEnabled = locManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true

                var lastLoc: android.location.Location? = null
                if (isGpsEnabled) {
                    lastLoc = locManager?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                }
                if (lastLoc == null && isNetEnabled) {
                    lastLoc = locManager?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                }
                if (lastLoc != null) {
                    pushLocation(lastLoc.latitude, lastLoc.longitude, lastLoc.speed, lastLoc.bearing)
                }
            } catch (_: SecurityException) {}
        }
    }

    fun pushLocation(lat: Double, lng: Double, speed: Float, bearing: Float) {
        val json = JSONObject()
        json.put("lat", lat)
        json.put("lng", lng)
        json.put("speedKmh", (speed * 3.6).toInt())
        json.put("bearing", bearing.toInt())
        postJs("window.AutoVerse && window.AutoVerse.updateLocation($json);")
    }

    fun pushMediaState(state: MediaUiState) {
        // Artwork decoding/PNG-encoding and settings reads are real IO — keep
        // them off the main thread; postJs hops back to main for evaluateJavascript.
        scope.launch(Dispatchers.Default) {
            val json = buildMediaStateJson(state)
            postJs("window.AutoVerse && window.AutoVerse.updateMediaState($json);")
        }
    }

    private fun buildMediaStateJson(state: MediaUiState): JSONObject {
        val json = JSONObject()
        val hasAccess = isNotificationAccessEnabled(context)
        json.put("accessRequired", !hasAccess)
        when (state) {
            is MediaUiState.Active -> {
                json.put("isActive", true)
                json.put("isPlaying", state.status.isPlaying)
                json.put("title", state.title ?: "Unknown Track")
                json.put("artist", state.artist ?: "Unknown Artist")
                json.put("album", state.album ?: "")
                json.put("packageName", state.packageName)
                json.put("appLabel", state.appLabel ?: "")
                state.progress?.let {
                    json.put("positionMs", it.positionMs)
                    json.put("durationMs", it.durationMs)
                }
                state.embeddedArtwork?.let { imgBmp ->
                    encodeBitmapBase64(imgBmp.asAndroidBitmap())?.let { json.put("artworkBase64", it) }
                }
                if (!json.has("artworkBase64") && state.artworkUri != null) {
                    val uriStr = state.artworkUri
                    if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                        json.put("artworkUrl", uriStr)
                    } else {
                        try {
                            val uri = Uri.parse(uriStr)
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                BitmapFactory.decodeStream(input)?.let { bmp ->
                                    encodeBitmapBase64(bmp)?.let { json.put("artworkBase64", it) }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            is MediaUiState.AccessRequired -> {
                json.put("isActive", false)
                json.put("isPlaying", false)
                json.put("accessRequired", true)
            }
            else -> {
                json.put("isActive", false)
                json.put("isPlaying", false)
            }
        }
        return json
    }

    /** PNG data-URL encoding with a hard downscale cap so huge cover art cannot stall the cockpit. */
    private fun encodeBitmapBase64(source: Bitmap, maxEdge: Int = 512): String? {
        return try {
            val longest = maxOf(source.width, source.height)
            val bmp = if (longest > maxEdge) {
                val scale = maxEdge.toFloat() / longest
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                source
            }
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 85, stream)
            "data:image/png;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun pushAppListState(state: AppListState) {
        if (state !is AppListState.Ready) return
        scope.launch(Dispatchers.IO) {
            val arr = JSONArray()
            for (app in state.apps) {
                val item = JSONObject()
                item.put("packageName", app.packageName)
                item.put("activityName", app.activityName)
                item.put("label", app.label)
                item.put("category", app.category.name.lowercase())
                val icon = getAppIconBase64(app)
                if (icon != null) {
                    item.put("iconBase64", icon)
                }
                arr.put(item)
            }
            mainHandler.post {
                postJs("window.AutoVerse && window.AutoVerse.updateInstalledApps($arr);")
            }
        }
    }

    fun pushConnectivity(status: ConnectivityStatus) {
        val json = JSONObject()
        json.put("wifiConnected", status.wifi == LinkState.CONNECTED)
        json.put("bluetoothConnected", status.bluetooth == LinkState.CONNECTED)
        json.put("deviceName", if (status.bluetooth == LinkState.CONNECTED) "Connected Phone" else "Hands-Free")
        postJs("window.AutoVerse && window.AutoVerse.updateConnectivity($json);")
    }

    private fun postJs(script: String) {
        mainHandler.post {
            webView.evaluateJavascript(script, null)
        }
    }
}
