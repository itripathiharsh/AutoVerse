package com.autoverse.launcher.feature.car

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.SwitchRow
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.remote.PhoneRemoteController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Car → Phone: three optional ways to use the phone with the car, all off
 * the main screens. Nothing here runs until the driver turns it on.
 */
@Composable
internal fun PhoneSection() {
    val context = LocalContext.current
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
    ) {
        Text("Phone", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Text(
            "Optional extras — use your phone with the car without Android Auto or any app on the phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        RemoteCard()
        BluetoothCard(onOpen = { openSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS) })
        SpotifyCard()
    }
}

@Composable
private fun RemoteCard() {
    val context = LocalContext.current
    val colors = LocalAutoVerseColors.current
    val remote = remember { PhoneRemoteController.get(context) }
    val enabled by remote.enabled.collectAsStateWithLifecycle()
    val url by remote.url.collectAsStateWithLifecycle()
    val lastPhone by remote.lastPhoneMs.collectAsStateWithLifecycle()

    // The hotspot can change while this page is open; keep the address fresh.
    LaunchedEffect(enabled) {
        while (enabled) {
            remote.refreshAddress()
            delay(5_000)
        }
    }

    Panel(icon = Icons.Outlined.QrCode2, title = "Phone remote (QR)") {
        SwitchRow(
            title = "Control the car from your phone",
            subtitle = "Scan a QR code — music, volume and sending places open in your phone's browser. iPhone or Android, nothing to install.",
            checked = enabled,
            onCheckedChange = remote::setEnabled,
        )
        if (enabled) {
            val address = url
            if (address == null) {
                Spacer(Modifier.height(AutoVerseSpacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Wifi, null, tint = colors.warning, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(AutoVerseSpacing.xs))
                    Text(
                        "Connect this unit to your phone's hotspot (or the same Wi-Fi) first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(AutoVerseSpacing.xs))
                PrimaryAction(text = "Open Wi-Fi settings", onClick = { openSettings(context, Settings.ACTION_WIFI_SETTINGS) })
            } else {
                Spacer(Modifier.height(AutoVerseSpacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    QrImage(address, Modifier.size(196.dp))
                    Spacer(Modifier.width(AutoVerseSpacing.lg))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
                        Step(1, "Keep your phone's hotspot on (the car uses it for internet).")
                        Step(2, "Open the phone camera and point it at this code.")
                        Step(3, "Tap the link — the remote opens. Add it to your home screen for next time.")
                        val connected = lastPhone != 0L && SystemClock.elapsedRealtime() - lastPhone < 15_000
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (connected) colors.success else colors.textFaint),
                            )
                            Spacer(Modifier.width(AutoVerseSpacing.xs))
                            Text(
                                if (connected) "Phone connected" else "Waiting for a phone…",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (connected) colors.success else colors.textSecondary,
                            )
                        }
                        // Fallback when a camera won't scan: type the address and code.
                        val code = address.substringAfter('#')
                        Text(
                            "Can't scan? Open ${address.substringBefore('#').removePrefix("http://").trimEnd('/')} " +
                                "on the phone and enter ${code.chunked(5).joinToString(" ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textFaint,
                        )
                        TextAction(text = "New code (disconnects phones)", onClick = remote::newCode, quiet = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothCard(onOpen: () -> Unit) {
    Panel(icon = Icons.Outlined.Bluetooth, title = "Phone music over Bluetooth") {
        Column(verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
            Step(1, "Tap Pair phone below and turn on Bluetooth on your phone.")
            Step(2, "Pick this car on your phone and accept the code on both screens.")
            Step(3, "Done for good — next time your phone connects by itself when the car starts.")
            Text(
                "Spotify, YouTube Music, Apple Music or any app playing on the phone then shows on Home and Music with play, pause and skip. Works with iPhone and Android.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalAutoVerseColors.current.textSecondary,
            )
            Spacer(Modifier.height(AutoVerseSpacing.xxs))
            PrimaryAction(text = "Pair phone", onClick = onOpen, icon = Icons.Outlined.Bluetooth)
        }
    }
}

@Composable
private fun SpotifyCard() {
    val context = LocalContext.current
    val colors = LocalAutoVerseColors.current
    val installed = remember { context.packageManager.getLaunchIntentForPackage(SPOTIFY) != null }
    Panel(icon = Icons.Outlined.Speaker, title = "Spotify Connect") {
        Text(
            if (installed) {
                "Spotify is on this unit. Sign in once, then on your phone's Spotify tap the devices icon and pick this car — the music moves to the car with one tap, and you keep controlling it from the phone."
            } else {
                "Install Spotify on this unit (Apps → Play Store) and sign in once. Then your phone's Spotify can send music to the car with one tap, and you keep controlling it from the phone."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        if (installed) {
            Spacer(Modifier.height(AutoVerseSpacing.xs))
            TextAction(
                text = "Open Spotify",
                onClick = {
                    context.packageManager.getLaunchIntentForPackage(SPOTIFY)?.let {
                        context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
            )
        }
    }
}

@Composable
private fun Panel(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.tile)
            .background(colors.surfaceRaised)
            .padding(AutoVerseSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(AutoVerseSpacing.xs))
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
        }
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        content()
    }
}

@Composable
private fun Step(n: Int, text: String) {
    val colors = LocalAutoVerseColors.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.surfaceHighlight),
            contentAlignment = Alignment.Center,
        ) {
            Text(n.toString(), style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
        }
        Spacer(Modifier.width(AutoVerseSpacing.xs))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
    }
}

/** The QR on a white quiet-zone tile (dark-on-light scans most reliably). */
@Composable
private fun QrImage(content: String, modifier: Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, content) { value = withContext(Dispatchers.Default) { qrBitmap(content) } }
    Box(
        modifier = modifier
            .clip(AutoVerseShapes.row)
            .background(Color.White)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        bitmap?.let {
            Image(it, contentDescription = "QR code for AutoVerse Remote", filterQuality = FilterQuality.None, modifier = Modifier.size(176.dp))
        }
    }
}

private fun qrBitmap(content: String): ImageBitmap? = try {
    val matrix = QRCodeWriter().encode(
        content, BarcodeFormat.QR_CODE, 0, 0,
        mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
    )
    val bmp = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    for (x in 0 until matrix.width) {
        for (y in 0 until matrix.height) {
            bmp.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    bmp.asImageBitmap()
} catch (_: Exception) {
    null
}

private fun openSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}

private const val SPOTIFY = "com.spotify.music"
