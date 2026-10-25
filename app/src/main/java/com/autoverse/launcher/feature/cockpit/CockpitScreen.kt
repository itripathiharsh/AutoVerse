package com.autoverse.launcher.feature.cockpit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.MainViewModel
import com.autoverse.launcher.core.bridge.AutoVerseBridge

private fun voiceIntentFor(mode: String): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            when (mode) {
                "apps" -> "Which app should I open?"
                "navigation" -> "Where to?"
                else -> "Say a command \u2014 e.g. open Spotify, or navigate to work"
            },
        )
    }

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CockpitScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val mediaState by viewModel.mediaState.collectAsStateWithLifecycle()
    val appListState by viewModel.appListState.collectAsStateWithLifecycle()
    val connectivity by viewModel.connectivity.collectAsStateWithLifecycle()
    val obdState by viewModel.obdState.collectAsStateWithLifecycle()
    val offlineVoiceState by viewModel.offlineVoiceState.collectAsStateWithLifecycle()
    val performanceSpec by viewModel.performanceSpec.collectAsStateWithLifecycle()

    // Holder survives recomposition; a plain `var x = remember { null }` re-nulls
    // on every recomposition, silently dropping the bridge for onPageFinished
    // and ON_RESUME callbacks.
    val bridgeHolder = remember { arrayOfNulls<AutoVerseBridge>(1) }
    val voiceModeHolder = remember { arrayOf("command") }

    val obdPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        bridgeHolder[0]?.pushObdPermissionResult(granted)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        bridgeHolder[0]?.pushMicPermissionResult(granted)
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val mode = voiceModeHolder[0]
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            bridgeHolder[0]?.pushVoiceResult(text, mode, if (text.isNullOrBlank()) "nothing heard" else null)
        } else {
            bridgeHolder[0]?.pushVoiceResult(null, mode, "voice input cancelled")
        }
    }
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Pure 60 FPS Hardware Layer Compositing
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            setBackgroundColor(0xFF06090E.toInt())

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = false
                databaseEnabled = true
                setGeolocationEnabled(true)
                mediaPlaybackRequiresUserGesture = false
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            webChromeClient = object : WebChromeClient() {
                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: android.webkit.GeolocationPermissions.Callback?
                ) {
                    callback?.invoke(origin, true, false)
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    bridgeHolder[0]?.requestInitialState()
                }
            }
        }
    }

    val bridge = remember(webView) {
        AutoVerseBridge(context, viewModel, webView, coroutineScope).also {
            bridgeHolder[0] = it
            it.micPermissionHandler = {
                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
            it.obdPermissionHandler = {
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    obdPermissionLauncher.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    it.pushObdPermissionResult(true)
                }
            }
            it.voiceRequestHandler = { mode ->
                voiceModeHolder[0] = mode
                try {
                    voiceLauncher.launch(voiceIntentFor(mode))
                } catch (_: ActivityNotFoundException) {
                    it.pushVoiceResult(null, mode, "no voice recognition service on this device")
                }
            }
            webView.addJavascriptInterface(it, "AutoVerseAndroid")
            webView.loadUrl("file:///android_asset/cockpit/index.html")
        }
    }

    LaunchedEffect(mediaState) {
        bridge.pushMediaState(mediaState)
    }

    LaunchedEffect(appListState) {
        bridge.pushAppListState(appListState)
    }

    LaunchedEffect(connectivity) {
        bridge.pushConnectivity(connectivity)
    }

    LaunchedEffect(obdState) {
        bridge.pushObdState(obdState)
    }

    LaunchedEffect(offlineVoiceState) {
        bridge.pushOfflineVoiceState(offlineVoiceState)
    }

    LaunchedEffect(performanceSpec) {
        bridge.pushWorkloadProfile(performanceSpec.profile.name)
    }

    LaunchedEffect(Unit) {
        viewModel.offlineVoiceCommands.collect { command ->
            val payload = org.json.JSONObject().put("ok", true).put("text", command).put("mode", "command")
            webView.evaluateJavascript(
                "window.AutoVerse && window.AutoVerse.onVoiceResult($payload)",
                null,
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                bridgeHolder[0]?.requestInitialState()
                viewModel.onAppForeground()
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onAppBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            bridgeHolder[0] = null
            webView.stopLoading()
            webView.removeJavascriptInterface("AutoVerseAndroid")
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier.fillMaxSize()
    )
}
