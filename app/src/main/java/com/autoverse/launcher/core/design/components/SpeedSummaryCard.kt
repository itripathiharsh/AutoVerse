package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.location.GpsStatus
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.weather.WeatherUiState

/** Source line under the speed: what the number is based on, in plain words. */
fun gpsStatusLabel(status: GpsStatus, hasLocationPermission: Boolean): String = when {
    !hasLocationPermission || status == GpsStatus.NO_ACCESS -> "No location access"
    status == GpsStatus.LIVE -> "GPS · live"
    status == GpsStatus.STALE -> "GPS signal lost"
    status == GpsStatus.PROVIDER_OFF -> "Location is switched off"
    else -> "GPS · waiting for fix"
}

/**
 * The Home speed panel: GPS-derived speed with its source stated, an honest
 * unavailable state ("--" plus why) instead of a frozen or zero reading, and
 * one optional weather line when there is room. Call alerts are offered here
 * as an opt-in; the panel never asks for phone access by itself.
 */
@Composable
fun SpeedSummaryCard(
    speedometerData: SpeedometerData,
    hasLocationPermission: Boolean,
    onOpenSpeedometer: () -> Unit,
    modifier: Modifier = Modifier,
    onRequestLocationPermission: () -> Unit = {},
    weatherState: WeatherUiState? = null,
    compact: Boolean = false,
    hasCallPermission: Boolean = true,
    onRequestCallPermission: () -> Unit = {},
) {
    val colors = LocalAutoVerseColors.current
    val status = if (hasLocationPermission) speedometerData.gpsStatus else GpsStatus.NO_ACCESS
    val speed = speedometerData.currentSpeedKmh.takeIf { status == GpsStatus.LIVE }

    WidgetContainer(
        title = "SPEED",
        icon = Icons.Outlined.Speed,
        modifier = modifier,
        onClick = onOpenSpeedometer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = AutoVerseSpacing.xxs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                Text(
                    text = speed?.toString() ?: "--",
                    fontSize = if (compact) 40.sp else 52.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (speed != null) colors.textPrimary else colors.textFaint,
                    lineHeight = if (compact) 42.sp else 54.sp,
                    maxLines = 1,
                )
                Text(
                    text = "km/h",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accent,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                Icon(
                    imageVector = when (status) {
                        GpsStatus.LIVE -> Icons.Outlined.GpsFixed
                        GpsStatus.NO_ACCESS, GpsStatus.PROVIDER_OFF -> Icons.Outlined.GpsOff
                        else -> Icons.Outlined.GpsNotFixed
                    },
                    contentDescription = null,
                    modifier = Modifier.size(AutoVerseIcons.inline),
                    tint = when (status) {
                        GpsStatus.LIVE -> colors.success
                        GpsStatus.STALE -> colors.warning
                        else -> colors.textFaint
                    },
                )
                Text(
                    text = gpsStatusLabel(status, hasLocationPermission),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            if (!compact && weatherState is WeatherUiState.Available) {
                Text(
                    text = "${weatherState.temperatureC}°C · ${weatherState.conditionText}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when {
                !hasLocationPermission -> TextAction(text = "Allow location", onClick = onRequestLocationPermission)
                !hasCallPermission && !compact ->
                    TextAction(text = "Show incoming calls here", onClick = onRequestCallPermission)
            }
        }
    }
}
