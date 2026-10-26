package com.autoverse.launcher.feature.speedometer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.SectionLabel
import com.autoverse.launcher.core.design.components.StatusChip
import com.autoverse.launcher.core.design.components.StatusTone
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.weather.WeatherUiState

@Composable
fun SpeedometerScreen(
    speedometerData: SpeedometerData,
    weatherState: WeatherUiState,
    onResetTrip: () -> Unit = {},
    onRefreshWeather: () -> Unit = {},
    onStartSpeedometer: () -> Unit = {},
    onStopSpeedometer: () -> Unit = {},
    onOpenHome: () -> Unit = {},
    onOpenAppDrawer: () -> Unit = {},
    onOpenNavigation: () -> Unit = {},
    onOpenObd: () -> Unit = {},
    onOpenDiagnostics: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current

    DisposableEffect(Unit) {
        onStartSpeedometer()
        onDispose {
            onStopSpeedometer()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = AutoVerseSpacing.screenHorizontal,
                vertical = AutoVerseSpacing.screenVertical,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
        ) {
            AppNavigationRail(
                selected = RailDestination.SPEEDOMETER,
                onOpenHome = onOpenHome,
                onOpenAppDrawer = onOpenAppDrawer,
                onOpenNavigation = onOpenNavigation,
                onOpenSettings = onOpenSettings,
                onOpenSpeedometer = {},
                onOpenObd = onOpenObd,
                onOpenDiagnostics = onOpenDiagnostics,
                modifier = Modifier
                    .width(AutoVerseSpacing.railWidth)
                    .fillMaxHeight(),
            )

            // Main Content Area
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
            ) {
                // Primary Speedometer Gauge Panel
                SpeedometerGaugeCard(
                    speedometerData = speedometerData,
                    onOpenNavigation = onOpenNavigation,
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                )

                // Secondary Driving Metrics & Weather Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
                ) {
                    TripMetricsCard(
                        speedometerData = speedometerData,
                        onResetTrip = onResetTrip,
                        modifier = Modifier.weight(1f),
                    )

                    WeatherCard(
                        weatherState = weatherState,
                        onRefreshWeather = onRefreshWeather,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedometerGaugeCard(
    speedometerData: SpeedometerData,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val motion = com.autoverse.launcher.core.design.tokens.LocalAutoVerseMotion.current
    val speed = speedometerData.currentSpeedKmh
    val targetSpeed = speed?.toFloat() ?: 0f
    val animatedSpeed = if (motion.standardMs == 0) {
        targetSpeed
    } else {
        animateFloatAsState(
            targetValue = targetSpeed,
            animationSpec = tween(motion.fastMs),
            label = "speedAnimation",
        ).value
    }

    WidgetContainer(
        title = "SPEEDOMETER",
        icon = Icons.Outlined.Speed,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AutoVerseSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top Status Bar: GPS & Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(
                    icon = if (speedometerData.isGpsActive) Icons.Outlined.GpsFixed else Icons.Outlined.GpsNotFixed,
                    label = if (speedometerData.isGpsActive) "GPS LOCKED" else "GPS SEARCHING",
                    tone = if (speedometerData.isGpsActive) StatusTone.SUCCESS else StatusTone.WARNING,
                )

                if (speedometerData.accuracyMeters != null) {
                    Text(
                        text = "±${speedometerData.accuracyMeters.toInt()}m accuracy",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textFaint,
                    )
                }
            }

            // Central Radial Gauge with Digital Speed
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                SpeedArcCanvas(
                    currentSpeed = animatedSpeed,
                    maxSpeed = 220f,
                    accentColor = colors.accent,
                    trackColor = colors.surfaceHighlight,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (speed != null) {
                        Text(
                            text = "$speed",
                            fontSize = 68.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            lineHeight = 70.sp,
                        )
                    } else {
                        Text(
                            text = "---",
                            fontSize = 62.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textFaint,
                            lineHeight = 64.sp,
                        )
                    }

                    Text(
                        text = "KM / H",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent,
                        letterSpacing = 2.sp,
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (speed == null) "GPS Stationary" else "GPS Live",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
            }

            // Bottom Actions & Max Speed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "MAX SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textFaint,
                    )
                    Text(
                        text = "${speedometerData.maxSpeedKmh} km/h",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                }

                PrimaryAction(
                    text = "Open Navigation",
                    onClick = onOpenNavigation,
                )
            }
        }
    }
}

@Composable
private fun SpeedArcCanvas(
    currentSpeed: Float,
    maxSpeed: Float,
    accentColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val strokeWidth = 16.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )

        val startAngle = 140f
        val sweepAngle = 260f

        // Background Track Arc
        drawArc(
            color = trackColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        // Active Speed Arc
        val progress = (currentSpeed / maxSpeed).coerceIn(0f, 1f)
        val activeSweep = sweepAngle * progress

        if (activeSweep > 0f) {
            drawArc(
                color = accentColor,
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun TripMetricsCard(
    speedometerData: SpeedometerData,
    onResetTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current

    WidgetContainer(
        title = "TRIP DATA",
        icon = Icons.Outlined.Timer,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AutoVerseSpacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
            ) {
                // Trip Distance Metric
                MetricBlock(
                    label = "DISTANCE",
                    value = speedometerData.tripDistanceFormatted,
                    modifier = Modifier.weight(1f),
                )

                // Trip Duration Metric
                MetricBlock(
                    label = "DURATION",
                    value = speedometerData.tripDurationFormatted,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "GPS Trip statistics",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textFaint,
                )

                TextAction(
                    text = "Reset Trip",
                    onClick = onResetTrip,
                )
            }
        }
    }
}

@Composable
private fun MetricBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Box(
        modifier = modifier
            .clip(AutoVerseShapes.tile)
            .background(colors.surfaceHighlight)
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.sm),
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeatherCard(
    weatherState: WeatherUiState,
    onRefreshWeather: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current

    WidgetContainer(
        title = "CABIN & WEATHER",
        icon = Icons.Outlined.Cloud,
        modifier = modifier,
    ) {
        when (weatherState) {
            is WeatherUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = colors.accent,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }

            is WeatherUiState.Available -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = AutoVerseSpacing.xs),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
                        ) {
                            Icon(
                                imageVector = weatherIconFor(weatherState.conditionCode),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(36.dp),
                            )
                            Column {
                                Text(
                                    text = "${weatherState.temperatureC}°C",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = weatherState.conditionText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Air,
                                    contentDescription = null,
                                    tint = colors.textFaint,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "${weatherState.windSpeedKmh} km/h wind",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                )
                            }
                            Text(
                                text = "Updated ${weatherState.lastUpdatedFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textFaint,
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Live Open-Meteo feed",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textFaint,
                        )
                        TextAction(
                            text = "Refresh",
                            onClick = onRefreshWeather,
                        )
                    }
                }
            }

            is WeatherUiState.Unavailable -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = AutoVerseSpacing.xs),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Weather unavailable",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = weatherState.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textFaint,
                        )
                    }

                    TextAction(
                        text = "Retry",
                        onClick = onRefreshWeather,
                    )
                }
            }
        }
    }
}

private fun weatherIconFor(code: Int): ImageVector = when (code) {
    0, 1 -> Icons.Outlined.WbSunny
    2, 3 -> Icons.Outlined.Cloud
    else -> Icons.Outlined.Thermostat
}
