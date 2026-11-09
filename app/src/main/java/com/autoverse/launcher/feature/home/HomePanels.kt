package com.autoverse.launcher.feature.home

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.GpsNotFixed
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.design.tokens.LocalAutoVerseMotion
import com.autoverse.launcher.core.location.GpsStatus
import com.autoverse.launcher.core.location.SpeedometerData
import com.autoverse.launcher.core.media.MediaCommand
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.isPlaying
import com.autoverse.launcher.core.media.mediaToggleCommand
import com.autoverse.launcher.core.permissions.LocationFix
import com.autoverse.launcher.core.permissions.locationFixFor
import com.autoverse.launcher.core.permissions.openLocationServiceSettings
import com.autoverse.launcher.core.weather.WeatherUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home's music panel: the album art fills the left, the whole panel is tinted
 * with the artwork's colour, and the controls are big enough to hit while
 * driving. Tapping the panel (not a control) opens the Music page.
 */
@Composable
internal fun HomeMusicPanel(
    media: MediaSection,
    onOpenMusic: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val motion = LocalAutoVerseMotion.current
    val state = media.state
    val artwork by produceState<ImageBitmap?>(null, state) { value = media.loadArtwork(state) }
    val tint by produceState<Color?>(null, artwork) { value = artwork?.let { dominantColor(it) } }
    val glow by animateColorAsState(
        targetValue = tint?.let { lerp(colors.surface, it, 0.45f) } ?: colors.surface,
        animationSpec = tween(motion.standardMs.coerceAtLeast(1) * 2),
        label = "musicTint",
    )

    Box(
        modifier = modifier
            .clip(AutoVerseShapes.card)
            .background(Brush.linearGradient(listOf(glow, colors.surface)))
            .clickable(onClick = onOpenMusic)
            .padding(AutoVerseSpacing.md),
    ) {
        when (state) {
            MediaUiState.AccessRequired -> EmptyMusic(
                title = "Music control is off",
                action = "Allow",
                onAction = media.onOpenAccess,
            )
            is MediaUiState.Active -> BoxWithConstraints(Modifier.fillMaxSize()) {
                val artSize = (if (compact) maxHeight * 0.5f else maxHeight * 0.58f).coerceAtMost(maxWidth * 0.42f)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val art = artwork
                        // Without cover art: a soft accent gradient tile, not a grey placeholder.
                        Box(
                            modifier = Modifier
                                .size(artSize)
                                .clip(AutoVerseShapes.row)
                                .background(
                                    if (art != null) {
                                        Brush.linearGradient(listOf(colors.surfaceHighlight, colors.surfaceHighlight))
                                    } else {
                                        Brush.linearGradient(listOf(colors.accent.copy(alpha = 0.55f), colors.surfaceHighlight))
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (art != null) {
                                Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Icon(Icons.Outlined.MusicNote, null, tint = colors.textPrimary.copy(alpha = 0.85f), modifier = Modifier.size(artSize * 0.36f))
                            }
                        }
                        Spacer(Modifier.width(AutoVerseSpacing.md))
                        AnimatedContent(
                            targetState = state.title to state.artist,
                            transitionSpec = { fadeIn(tween(motion.standardMs)) togetherWith fadeOut(tween(motion.fastMs)) },
                            label = "track",
                            modifier = Modifier.weight(1f),
                        ) { (title, artist) ->
                            // Never "Google / Google": the app's name shows once, as the
                            // source line, and only when the track has its own title.
                            val heading = title ?: state.appLabel ?: "Playing"
                            val sub = artist?.takeIf { it != heading }
                                ?: state.appLabel?.takeIf { title != null && it != heading }
                            Column {
                                Text(
                                    heading,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (sub != null) {
                                    Text(
                                        sub,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    state.progress?.let { p ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(colors.outline),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(p.fraction)
                                    .fillMaxHeight()
                                    .background(colors.accent),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundControl(Icons.Outlined.SkipPrevious, "Previous", state.controls.canSkipPrevious, small = true) {
                            media.onCommand(MediaCommand.PREVIOUS)
                        }
                        RoundControl(
                            if (state.status.isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            if (state.status.isPlaying) "Pause" else "Play",
                            state.controls.canTogglePlayPause,
                            primary = true,
                        ) { media.onCommand(mediaToggleCommand(state.status)) }
                        RoundControl(Icons.Outlined.SkipNext, "Next", state.controls.canSkipNext, small = true) {
                            media.onCommand(MediaCommand.NEXT)
                        }
                    }
                }
            }
            else -> EmptyMusic(title = "Nothing playing", action = "Pick music", onAction = onOpenMusic)
        }
    }
}

@Composable
private fun EmptyMusic(title: String, action: String, onAction: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.MusicNote, null, tint = colors.textFaint, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        PrimaryAction(text = action, onClick = onAction)
    }
}

@Composable
private fun RoundControl(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    primary: Boolean = false,
    small: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val size = if (primary) 68.dp else if (small) 52.dp else 56.dp
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (primary) colors.textPrimary else colors.surfaceHighlight)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = when {
                !enabled -> colors.textFaint
                primary -> colors.backgroundTop
                else -> colors.textPrimary
            },
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** One representative colour of the artwork (1×1 downscale, off the main thread). */
private suspend fun dominantColor(art: ImageBitmap): Color? = withContext(Dispatchers.Default) {
    try {
        val px = Bitmap.createScaledBitmap(art.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false), 1, 1, true)
            .getPixel(0, 0)
        Color(px)
    } catch (_: Exception) {
        null
    }
}

/**
 * Home's speed panel: one huge number you can read at a glance, the road's
 * speed limit as a real road sign beside it (number turns red when over),
 * and the weather and today's distance underneath. Location problems get a
 * single fix button instead of technical text.
 */
@Composable
internal fun HomeSpeedPanel(
    driving: DrivingSection,
    speedLimitKmh: Int?,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val context = LocalContext.current
    val data: SpeedometerData = driving.speedometerData
    val status = if (driving.hasLocationPermission) data.gpsStatus else GpsStatus.NO_ACCESS
    val speed = data.currentSpeedKmh.takeIf { status == GpsStatus.LIVE } ?: if (status == GpsStatus.LIVE) 0 else null
    val over = speed != null && speedLimitKmh != null && speed > speedLimitKmh + 2
    val speedColor by animateColorAsState(if (over) colors.danger else colors.textPrimary, label = "speedColor")

    Column(
        modifier = modifier
            .clip(AutoVerseShapes.card)
            .background(colors.surface)
            .then(if (over) Modifier.border(2.dp, colors.danger, AutoVerseShapes.card) else Modifier)
            .clickable(onClick = driving.onOpenSpeedometer)
            .padding(AutoVerseSpacing.md),
    ) {
        // The number is the hero: centred, huge, tabular. Without a fix it
        // reads a calm dim "0", never dashes.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = (speed ?: 0).toString(),
                    style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                    color = if (speed != null) speedColor else colors.textFaint,
                    maxLines = 1,
                )
                Spacer(Modifier.width(AutoVerseSpacing.xs))
                Text(
                    "km/h",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = if (compact) 6.dp else 12.dp),
                )
            }
            if (speedLimitKmh != null) {
                SpeedLimitSign(speedLimitKmh, size = if (compact) 52 else 64)
            }
        }

        // One quiet status line: a location fix button, the GPS state, or the
        // weather condition plus today's distance (the temperature is in the rail).
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            val fix = locationFixFor(status != GpsStatus.NO_ACCESS, status != GpsStatus.PROVIDER_OFF)
            val w = driving.weatherState
            when {
                fix == LocationFix.GRANT -> TextAction(text = fix.label, onClick = driving.onRequestLocationPermission)
                fix == LocationFix.TURN_ON -> TextAction(text = fix.label, onClick = { openLocationServiceSettings(context) })
                status != GpsStatus.LIVE -> {
                    Icon(Icons.Outlined.GpsNotFixed, null, tint = colors.textFaint, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(AutoVerseSpacing.xs))
                    Text("Waiting for GPS", style = MaterialTheme.typography.bodyMedium, color = colors.textFaint)
                }
                w is WeatherUiState.Available -> {
                    Icon(
                        if (w.conditionCode <= 1) Icons.Outlined.WbSunny else Icons.Outlined.Cloud,
                        null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(AutoVerseSpacing.xs))
                    Text(
                        "${w.temperatureC}° · ${w.conditionText}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (data.tripDistanceMeters >= 100) {
                Text(
                    "${data.tripDistanceFormatted} today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** A real Indian/European speed-limit sign: red ring, white disc, black number. */
@Composable
internal fun SpeedLimitSign(limitKmh: Int, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Color(0xFFD32F2F))
            .padding((size / 9).dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            limitKmh.toString(),
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.36f).sp,
        )
    }
}
