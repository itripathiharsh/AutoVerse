package com.autoverse.launcher.feature.probe

import android.content.ComponentName
import android.os.IBinder
import android.view.SurfaceView
import android.os.SystemClock
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AutoVerseScreen
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.mapdisplay.IMapsDisplayService
import com.autoverse.launcher.core.mapdisplay.MapDisplayCommands
import com.autoverse.launcher.core.mapdisplay.TouchMapper
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

private const val MAX_LOG_EVENTS = 14
private const val SHIZUKU_REQUEST_CODE = 4701

/** Virtual display resolution; density 160 keeps Maps' UI compact. */
private const val DISPLAY_W = 800
private const val DISPLAY_H = 500
private const val DISPLAY_DENSITY = 160

/**
 * 0.28.4 — the virtual-display proof of concept. If Shizuku is granted, this
 * screen creates a virtual display rendering into the card below, launches
 * the installed Google Maps onto it, and forwards touches — the real Maps UI
 * living inside AutoVerse's layout, key-free. Everything logs; nothing here
 * changes the shipped screens until this proves out on hardware.
 */
@Composable
fun MapDisplayProbeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = LocalAutoVerseColors.current
    val log = remember { mutableStateListOf<String>() }
    val serviceRef = remember { AtomicReference<IMapsDisplayService?>(null) }
    val holders = remember { mutableStateListOf<AtomicReference<IMapsDisplayService?>>() }
    var displayId by remember { mutableIntStateOf(-1) }
    var surface by remember { mutableStateOf<Surface?>(null) }
    var surfaceReady by remember { mutableStateOf(false) }
    var shizukuState by remember { mutableStateOf("checking…") }
    var viewSize by remember { mutableStateOf(0f to 0f) }

    fun appendLog(line: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        log.add(0, "$stamp  $line")
        while (log.size > MAX_LOG_EVENTS) log.removeAt(log.size - 1)
    }

    LaunchedEffect(Unit) {
        while (true) {
            shizukuState = try {
                when {
                    !Shizuku.pingBinder() -> "not running — open the Shizuku app"
                    Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED -> "granted"
                    else -> "permission not granted"
                }
            } catch (failure: Exception) {
                "unavailable (${failure.javaClass.simpleName})"
            }
            delay(1_000)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val connected = serviceRef.get()
            val id = displayId
            try {
                if (connected != null && id >= 0) connected.destroyDisplay(id)
            } catch (_: Exception) {
                // The display also dies with the Shizuku server session.
            }
        }
    }

    fun startMaps() {
        if (displayId >= 0) {
            appendLog("Already running on display $displayId")
            return
        }
        if (!surfaceReady || surface == null) {
            appendLog("Surface not ready yet — wait a moment")
            return
        }
        val granted = try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
        if (!granted) {
            appendLog("Shizuku is not granted — request permission first")
            return
        }
        appendLog("Binding user service (runs as shell)…")
        val activeSurface: Surface = surface!!
        val args = Shizuku.UserServiceArgs(
            ComponentName(context, "com.autoverse.launcher.core.mapdisplay.MapDisplayService"),
        )
            .version(1)
            .processNameSuffix("mapdisplay")
        val holder = AtomicReference<IMapsDisplayService?>(null)
        holders.add(holder)
        Shizuku.bindUserService(args, object : android.content.ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                if (binder == null || !binder.pingBinder()) {
                    appendLog("Service bind returned nothing")
                    return
                }
                val connected = IMapsDisplayService.Stub.asInterface(binder)
                holder.set(connected)
                serviceRef.set(connected)
                try {
                    val id = connected.createDisplay(
                        DISPLAY_W, DISPLAY_H, DISPLAY_DENSITY, activeSurface,
                    )
                    displayId = id
                    appendLog("Virtual display created: id=$id ${DISPLAY_W}x${DISPLAY_H}")
                    // Blocking call on this binder thread is fine: the command
                    // returns as soon as `am` has fired the launch.
                    val code = connected.runShell(
                        MapDisplayCommands.launchMapsOnDisplay(id),
                    )
                    appendLog("Maps launch (am start) exit=$code on display $id")
                } catch (failure: Exception) {
                    appendLog("Display create failed: ${failure.message}")
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                serviceRef.set(null)
                appendLog("User service disconnected")
            }
        })
    }

    fun stopMaps() {
        val connected = serviceRef.get()
        try {
            if (connected != null && displayId >= 0) {
                connected.destroyDisplay(displayId)
                appendLog("Display $displayId destroyed")
                displayId = -1
            } else {
                appendLog("Nothing to stop")
            }
        } catch (failure: Exception) {
            appendLog("Stop failed: ${failure.message}")
        }
    }

    AutoVerseScreen(
        title = "Map display probe (Shizuku)",
        onBack = onBack,
        modifier = modifier,
    ) {
        Text(
            text = "Proof of concept: Google Maps' real UI hosted inside AutoVerse on a " +
                "virtual display (shell privileges via Shizuku — no root, no API key). " +
                "Grant Shizuku, press Start, then touch the card to drive Maps.",
            style = MaterialTheme.typography.bodyMedium,
        )

        WidgetContainer(title = "SHIZUKU", icon = Icons.Outlined.Map) {
            Text(text = "Status: $shizukuState")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                TextAction(
                    text = "Request permission",
                    onClick = {
                        try {
                            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
                            appendLog("Permission requested — accept it in the dialog")
                        } catch (failure: Exception) {
                            appendLog("Permission request failed: ${failure.message}")
                        }
                    },
                )
                TextAction(
                    text = "Setup steps",
                    onClick = {
                        appendLog(
                            "1) install the Shizuku app 2) start it via wireless debugging " +
                                "3) return here and grant",
                        )
                    },
                )
            }
        }

        WidgetContainer(title = "LIVE TEST", icon = Icons.Outlined.Map) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(colors.surface)
                    .onSizeChanged { size ->
                        viewSize = size.width.toFloat() to size.height.toFloat()
                    }
                    .pointerInput(displayId) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val downTime = SystemClock.uptimeMillis()
                            val connected = serviceRef.get()
                            val id = displayId
                            if (connected == null || id < 0) {
                                // Nothing hosted yet — swallow the gesture.
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.all { !it.pressed }) break
                                }
                                return@awaitEachGesture
                            }
                            fun inject(position: Offset, action: Int) {
                                try {
                                    val event = MotionEvent.obtain(
                                        downTime,
                                        SystemClock.uptimeMillis(),
                                        action,
                                        TouchMapper.scaleX(position.x, viewSize.first, DISPLAY_W),
                                        TouchMapper.scaleY(position.y, viewSize.second, DISPLAY_H),
                                        0,
                                    )
                                    connected.injectMotionEvent(event, id)
                                } catch (failure: Exception) {
                                    appendLog("Touch inject failed: ${failure.message}")
                                }
                            }
                            inject(down.position, MotionEvent.ACTION_DOWN)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    inject(change.position, MotionEvent.ACTION_UP)
                                    break
                                }
                                inject(change.position, MotionEvent.ACTION_MOVE)
                            }
                        }
                    },
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { probeContext ->
                        SurfaceView(probeContext).apply {
                            holder.addCallback(object : SurfaceHolder.Callback {
                                override fun surfaceCreated(holder: SurfaceHolder) {
                                    surface = holder.surface
                                    surfaceReady = true
                                }

                                override fun surfaceChanged(
                                    holder: SurfaceHolder,
                                    format: Int,
                                    width: Int,
                                    height: Int,
                                ) = Unit

                                override fun surfaceDestroyed(holder: SurfaceHolder) {
                                    surfaceReady = false
                                    surface = null
                                }
                            })
                        }
                    },
                )
                if (!surfaceReady) {
                    Text(
                        text = "Map display area — waiting…",
                        modifier = Modifier.align(Alignment.Center),
                        color = colors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.height(AutoVerseSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
            ) {
                PrimaryAction(
                    text = "Start Maps on display",
                    onClick = { startMaps() },
                    modifier = Modifier.weight(1f),
                )
                TextAction(
                    text = "Stop",
                    onClick = { stopMaps() },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (log.isNotEmpty()) {
            WidgetContainer(title = "PROBE LOG (newest first)", icon = Icons.Outlined.Map) {
                log.forEach { event ->
                    Text(
                        text = event,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                    )
                }
                Spacer(Modifier.size(AutoVerseSpacing.xs))
                TextAction(text = "Clear log", onClick = { log.clear() })
            }
        }
    }
}
