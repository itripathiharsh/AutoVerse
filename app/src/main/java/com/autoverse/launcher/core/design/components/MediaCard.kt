package com.autoverse.launcher.core.design.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.design.tokens.LocalAutoVerseMotion
import com.autoverse.launcher.core.media.MediaPresentation
import com.autoverse.launcher.core.media.MediaPresentationMode
import com.autoverse.launcher.core.media.PlaybackStatus
import com.autoverse.launcher.core.media.isPlaying
import com.autoverse.launcher.core.media.playbackStatusLabel
import com.autoverse.launcher.core.performance.LocalPerformanceSpec
import com.autoverse.launcher.core.util.formatTrackTime

/**
 * Media surface. Renders one [MediaPresentation]: transport controls gated by
 * the session's reported actions, artwork when available, and explicit
 * resolution paths for access-required / no-session / error states. When
 * [dense] is true the status line, message and footer are dropped so the card
 * fits short split layouts without clipping.
 */
@Composable
fun MediaCard(
    presentation: MediaPresentation,
    modifier: Modifier = Modifier,
    artwork: ImageBitmap? = null,
    dense: Boolean = false,
    onPrevious: () -> Unit = {},
    onTogglePlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onOpenAccessSettings: () -> Unit = {},
    onBrowseApps: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    WidgetContainer(title = "MEDIA", icon = Icons.Outlined.MusicNote, modifier = modifier) {
        when (presentation.mode) {
            MediaPresentationMode.ACTIVE -> ActiveContent(
                presentation = presentation,
                artwork = artwork,
                dense = dense,
                onPrevious = onPrevious,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
            )

            else -> MessageContent(
                presentation = presentation,
                dense = dense,
                onOpenAccessSettings = onOpenAccessSettings,
                onBrowseApps = onBrowseApps,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun ColumnScope.ActiveContent(
    presentation: MediaPresentation,
    artwork: ImageBitmap?,
    dense: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val playing = presentation.status?.isPlaying == true
    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
    ) {
        ArtworkTile(artwork = artwork, dense = dense)
        Column(modifier = Modifier.weight(1f)) {
            if (!dense) {
                presentation.status?.let { status ->
                    playbackStatusLabel(status)?.let { label ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.inlineGap),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(AutoVerseIcons.dot)
                                    .clip(CircleShape)
                                    .background(statusColor(status)),
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.size(AutoVerseSpacing.xxs))
                    }
                }
            }
            Text(
                text = presentation.headline,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = if (dense) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            presentation.subline?.let { subline ->
                Text(
                    text = subline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    presentation.progress?.let { progress ->
        Spacer(Modifier.size(AutoVerseSpacing.md))
        MediaProgressBar(fraction = progress.fraction)
        if (!dense && progress.durationMs > 0L) {
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatTrackTime(progress.positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textFaint,
                )
                Text(
                    text = formatTrackTime(progress.durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textFaint,
                )
            }
        }
    }
    Spacer(Modifier.size(AutoVerseSpacing.lg))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(
            space = AutoVerseSpacing.sm,
            alignment = Alignment.CenterHorizontally,
        ),
    ) {
        TransportButton(
            icon = Icons.Outlined.SkipPrevious,
            contentDescription = "Previous",
            onClick = onPrevious,
            enabled = presentation.controls.canSkipPrevious,
        )
        TransportButton(
            icon = if (playing) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            onClick = onTogglePlay,
            variant = TransportVariant.PRIMARY,
            enabled = presentation.controls.canTogglePlayPause,
        )
        TransportButton(
            icon = Icons.Outlined.SkipNext,
            contentDescription = "Next",
            onClick = onNext,
            enabled = presentation.controls.canSkipNext,
        )
    }
    Spacer(Modifier.weight(1f))
    if (!dense) {
        presentation.footer?.let { footer ->
            Text(
                text = footer,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ArtworkTile(artwork: ImageBitmap?, dense: Boolean) {
    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current
    val motion = LocalAutoVerseMotion.current
    val tileSize = if (dense) {
        AutoVerseSpacing.transportSecondary
    } else {
        AutoVerseSpacing.transportPrimary
    }
    Box(
        modifier = Modifier
            .size(tileSize)
            .clip(AutoVerseShapes.small)
            .background(colors.surfaceHighlight),
        contentAlignment = Alignment.Center,
    ) {
        if (spec.artworkCrossfade) {
            Crossfade(
                targetState = artwork,
                animationSpec = tween(motion.standardMs),
                label = "artworkTile",
            ) { image ->
                ArtworkVisual(image = image, colors = colors)
            }
        } else {
            ArtworkVisual(image = artwork, colors = colors)
        }
    }
}

/** Artwork or placeholder fill for the media card's artwork tile. */
@Composable
private fun ArtworkVisual(image: ImageBitmap?, colors: AutoVerseColors) {
    if (image != null) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Icon(
            Icons.Outlined.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.standard),
            tint = colors.textFaint,
        )
    }
}

@Composable
private fun ColumnScope.MessageContent(
    presentation: MediaPresentation,
    dense: Boolean,
    onOpenAccessSettings: () -> Unit,
    onBrowseApps: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Spacer(Modifier.weight(1f))
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = presentation.headline,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!dense) {
            presentation.message?.let { message ->
                Spacer(Modifier.size(AutoVerseSpacing.md))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        presentationAction(
            mode = presentation.mode,
            onOpenAccessSettings = onOpenAccessSettings,
            onBrowseApps = onBrowseApps,
            onRetry = onRetry,
        )?.let { (label, onClick) ->
            Spacer(Modifier.size(AutoVerseSpacing.lg))
            PrimaryAction(text = label, onClick = onClick)
        }
    }
    Spacer(Modifier.weight(1f))
    if (!dense) {
        presentation.footer?.let { footer ->
            Text(
                text = footer,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textFaint,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The single resolution action for a non-active state. CONNECTING has none —
 * it resolves on its own.
 */
private fun presentationAction(
    mode: MediaPresentationMode,
    onOpenAccessSettings: () -> Unit,
    onBrowseApps: () -> Unit,
    onRetry: () -> Unit,
): Pair<String, () -> Unit>? = when (mode) {
    MediaPresentationMode.ACCESS_REQUIRED -> "Open system settings" to onOpenAccessSettings
    MediaPresentationMode.NO_SESSION -> "Browse apps" to onBrowseApps
    MediaPresentationMode.ERROR -> "Try again" to onRetry
    else -> null
}

@Composable
private fun MediaProgressBar(fraction: Float) {
    val colors = LocalAutoVerseColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(AutoVerseSpacing.xxs)
            .clip(AutoVerseShapes.pill)
            .background(colors.surfaceHighlight)
            .testTag("mediaProgress"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(AutoVerseShapes.pill)
                .background(colors.accent),
        )
    }
}

@Composable
private fun statusColor(status: PlaybackStatus): Color {
    val colors = LocalAutoVerseColors.current
    return when (status) {
        PlaybackStatus.PLAYING -> colors.success
        PlaybackStatus.BUFFERING -> colors.warning
        PlaybackStatus.ERROR -> colors.danger
        else -> colors.textFaint
    }
}
