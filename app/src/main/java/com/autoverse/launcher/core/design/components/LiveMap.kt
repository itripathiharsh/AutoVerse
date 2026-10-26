package com.autoverse.launcher.core.design.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.location.GpsStatus
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.map.LiveMapHost
import com.autoverse.launcher.core.map.MapStyle
import com.autoverse.launcher.core.performance.LocalPerformanceSpec

/**
 * The activity's single live map, or null where no live map may render
 * (previews, tests, the miniature settings preview — a second host would
 * steal the one MapView).
 */
val LocalLiveMapHost = staticCompositionLocalOf<LiveMapHost?> { null }

/** Creates the one [LiveMapHost] for this activity; call once, at the app root. */
@Composable
fun rememberLiveMapHost(): LiveMapHost {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val background = LocalAutoVerseColors.current.backgroundTop.toArgb()
    val host = remember(context, lifecycle) { LiveMapHost(context, lifecycle, background) }
    DisposableEffect(host) { onDispose { host.release() } }
    return host
}

/**
 * Live, GPU-rendered map (MapLibre + free OpenFreeMap tiles) following the
 * car. Shows the one shared [LiveMapHost] map; without a host it renders a
 * plain placeholder so previews and tests never create a GL surface.
 */
@Composable
fun LiveMap(
    speedometerData: SpeedometerData,
    hasLocationPermission: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val host = LocalLiveMapHost.current
    val night = colors.backgroundTop.luminance() < 0.5f
    val maxFps = LocalPerformanceSpec.current.mapMaxFps

    Box(
        modifier = modifier
            .clip(AutoVerseShapes.small)
            .background(colors.surface),
    ) {
        if (host != null) {
            LaunchedEffect(host, night) { host.setNight(night) }
            LaunchedEffect(host, maxFps) { host.setMaxFps(maxFps) }
            LaunchedEffect(host, speedometerData.fixElapsedMs, speedometerData.gpsStatus) {
                if (speedometerData.gpsStatus == GpsStatus.LIVE) host.updateLocation(speedometerData)
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    // The single MapView moves between Home and Maps; detach it
                    // from the screen that showed it last before adopting it.
                    (host.mapView.parent as? ViewGroup)?.removeView(host.mapView)
                    host.mapView
                },
            )
        }

        val note = when {
            !hasLocationPermission -> "No location access"
            speedometerData.gpsStatus == GpsStatus.PROVIDER_OFF -> "Location is switched off"
            speedometerData.gpsStatus == GpsStatus.STALE -> "GPS signal lost"
            speedometerData.latitude == null -> "Waiting for GPS…"
            else -> null
        }
        if (note != null) {
            MapOverlayText(text = note, modifier = Modifier.align(Alignment.TopStart))
        }
        if (host != null && host.trackingDismissed) {
            TransportButton(
                icon = Icons.Outlined.MyLocation,
                contentDescription = "Recenter map",
                onClick = host::recenter,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(AutoVerseSpacing.xs),
            )
        }
        // OpenStreetMap's licence requires visible attribution on the map.
        MapOverlayText(
            text = MapStyle.ATTRIBUTION,
            modifier = Modifier.align(Alignment.BottomEnd),
            small = true,
        )
    }
}

@Composable
private fun MapOverlayText(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    val colors = LocalAutoVerseColors.current
    Text(
        text = text,
        style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        color = colors.textSecondary,
        maxLines = 1,
        modifier = modifier
            .padding(AutoVerseSpacing.xxs)
            .clip(AutoVerseShapes.small)
            .background(colors.surface.copy(alpha = 0.85f))
            .padding(horizontal = AutoVerseSpacing.xs, vertical = 2.dp),
    )
}
