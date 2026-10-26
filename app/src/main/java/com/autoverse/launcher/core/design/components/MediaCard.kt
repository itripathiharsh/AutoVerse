package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.media.MediaPresentation
import com.autoverse.launcher.core.media.MediaPresentationMode
import com.autoverse.launcher.core.media.MediaQueueUi
import com.autoverse.launcher.core.media.PlaybackStatus
import com.autoverse.launcher.core.media.isPlaying
import com.autoverse.launcher.core.media.mediaQueueEntryLabel
import com.autoverse.launcher.core.media.playbackStatusLabel
import com.autoverse.launcher.core.util.formatTrackTime

/**
 * Media surface. Renders one [MediaPresentation]: transport controls gated by
 * the session's reported actions, artwork when available, and explicit
 * resolution paths for access-required / no-session / error states. When
 * [dense] is true the status line, message and footer are dropped so the card
 * fits short split layouts without clipping.
 *
 * When the active session exposes a queue, a plain two-option selector
 * switches between Now Playing and the queue list (no animated carousel).
 * Selecting a queue entry requests a skip-to-item; players that reject it
 * surface the standard command outcome.
 *
 * The artwork renders directly — no crossfade — so a track change is a
 * single-frame swap with no animated compositing.
 */
@Composable
fun MediaCard(
    presentation: MediaPresentation,
    modifier: Modifier = Modifier,
    artwork: ImageBitmap? = null,
    dense: Boolean = false,
    collapsed: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null,
    queue: MediaQueueUi? = null,
    queueTitle: String? = null,
    showQueue: Boolean = false,
    onViewChange: (Boolean) -> Unit = {},
    onSkipToQueue: (Long) -> Unit = {},
    onPrevious: () -> Unit = {},
    onTogglePlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onOpenAccessSettings: () -> Unit = {},
    onBrowseApps: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val hasQueue = queue != null && queue.entries.isNotEmpty() &&
        presentation.mode == MediaPresentationMode.ACTIVE
    WidgetContainer(
        title = "NOW PLAYING",
        icon = Icons.Outlined.MusicNote,
        modifier = modifier,
        collapsed = collapsed,
        onToggleCollapsed = onToggleCollapsed,
        contentPadding = PaddingValues(if (dense) AutoVerseSpacing.sm else AutoVerseSpacing.cardPadding),
    ) {
        // Dense panels move the Now Playing / Queue switch into the transport
        // row (a queue button) so the compact cockpit panel needs no extra row.
        if (hasQueue && (!dense || showQueue)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                TextAction(
                    text = "NOW PLAYING",
                    onClick = { onViewChange(false) },
                    enabled = showQueue,
                )
                TextAction(
                    text = queueTitleLabel(queueTitle, queue!!.total),
                    onClick = { onViewChange(true) },
                    enabled = !showQueue,
                )
            }
        }
        when (presentation.mode) {
            MediaPresentationMode.ACTIVE -> {
                if (showQueue && queue != null) {
                    QueueContent(queue = queue, onSkipToQueue = onSkipToQueue)
                } else {
                    ActiveContent(
                        presentation = presentation,
                        artwork = artwork,
                        dense = dense,
                        onPrevious = onPrevious,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onShowQueue = if (dense && hasQueue) ({ onViewChange(true) }) else null,
                    )
                }
            }

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

private fun queueTitleLabel(queueTitle: String?, total: Int): String =
    if (queueTitle.isNullOrBlank()) "QUEUE" else "QUEUE \u00b7 $queueTitle" + if (total > 0) " ($total)" else ""

@Composable
private fun ColumnScope.QueueContent(
    queue: MediaQueueUi,
    onSkipToQueue: (Long) -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
    ) {
        itemsIndexed(queue.entries, key = { _, entry -> entry.queueId }) { index, entry ->
            val active = index == queue.activeIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AutoVerseShapes.tile)
                    .background(if (active) colors.surfaceHighlight else Color.Transparent)
                    .clickable(
                        role = Role.Button,
                        onClick = { onSkipToQueue(entry.queueId) },
                    )
                    .padding(
                        horizontal = AutoVerseSpacing.xs,
                        vertical = AutoVerseSpacing.xxs,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mediaQueueEntryLabel(entry, index),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (active) colors.accent else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textFaint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (active) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = "Now playing",
                        modifier = Modifier.size(AutoVerseIcons.inline),
                        tint = colors.accent,
                    )
                }
            }
        }
    }
    if (queue.truncated) {
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        Text(
            text = "Showing first ${queue.entries.size} of ${queue.total} queue items.",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    onShowQueue: (() -> Unit)? = null,
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
        if (onShowQueue != null) {
            IconButton(onClick = onShowQueue) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.QueueMusic,
                    contentDescription = "Show queue",
                    tint = colors.textSecondary,
                )
            }
        }
    }
    presentation.progress?.let { progress ->
        Spacer(Modifier.size(if (dense) AutoVerseSpacing.xs else AutoVerseSpacing.md))
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
    Spacer(Modifier.size(if (dense) AutoVerseSpacing.xs else AutoVerseSpacing.lg))
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
        ArtworkVisual(image = artwork, colors = colors)
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
