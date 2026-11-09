package com.autoverse.launcher.feature.music

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.AutoVerseTextField
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.media.MediaCommand
import com.autoverse.launcher.core.media.MediaCommandOutcome
import com.autoverse.launcher.core.media.MediaUiState
import com.autoverse.launcher.core.media.mediaQueueEntryLabel
import com.autoverse.launcher.core.media.mediaToggleCommand
import com.autoverse.launcher.core.media.isPlaying
import com.autoverse.launcher.core.music.AppBrowseState
import com.autoverse.launcher.core.music.LibraryView
import com.autoverse.launcher.core.music.LocalTrack
import com.autoverse.launcher.core.music.MusicApp
import com.autoverse.launcher.core.music.MusicController
import com.autoverse.launcher.core.music.audioReadPermission
import com.autoverse.launcher.core.music.filterTracks
import com.autoverse.launcher.core.music.groupTracks
import com.autoverse.launcher.core.permissions.openMediaAccessSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAB_QUEUE = "queue"
private const val TAB_LOCAL = "local"

/**
 * The Music page: what's playing (any app, or AutoVerse's own player) with
 * big driver-friendly controls on the left; on the right the queue, the
 * songs on this unit / SD card / USB drive, and the libraries of other music
 * apps that allow it — so a song can be picked without leaving AutoVerse.
 */
@Composable
fun MusicScreen(
    music: MusicController,
    mediaState: MediaUiState,
    loadArtwork: suspend (MediaUiState) -> ImageBitmap?,
    onCommand: suspend (MediaCommand) -> MediaCommandOutcome,
    onSeek: suspend (Long) -> MediaCommandOutcome,
    onSkipToQueue: suspend (Long) -> MediaCommandOutcome,
    onOpenHome: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenSpeedometer: () -> Unit,
    onOpenObd: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    LaunchedEffect(Unit) { music.refresh() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = AutoVerseSpacing.screenHorizontal, vertical = AutoVerseSpacing.screenVertical),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
        ) {
            AppNavigationRail(
                selected = RailDestination.MUSIC,
                onOpenHome = onOpenHome,
                onOpenAppDrawer = onOpenAppDrawer,
                onOpenNavigation = onOpenNavigation,
                onOpenSettings = onOpenSettings,
                onOpenSpeedometer = onOpenSpeedometer,
                onOpenObd = onOpenObd,
                onOpenDiagnostics = onOpenDiagnostics,
                modifier = Modifier
                    .width(AutoVerseSpacing.railWidth)
                    .fillMaxHeight(),
            )
            NowPlayingPanel(
                state = mediaState,
                loadArtwork = loadArtwork,
                onCommand = onCommand,
                onSeek = onSeek,
                modifier = Modifier
                    .weight(0.42f)
                    .fillMaxHeight(),
            )
            LibraryPanel(
                music = music,
                mediaState = mediaState,
                onSkipToQueue = onSkipToQueue,
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight(),
            )
        }
    }
}

// ---- left: now playing --------------------------------------------------------

@Composable
private fun NowPlayingPanel(
    state: MediaUiState,
    loadArtwork: suspend (MediaUiState) -> ImageBitmap?,
    onCommand: suspend (MediaCommand) -> MediaCommandOutcome,
    onSeek: suspend (Long) -> MediaCommandOutcome,
    modifier: Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active = state as? MediaUiState.Active
    val artwork by produceState<ImageBitmap?>(null, state) { value = loadArtwork(state) }
    var message by remember { mutableStateOf<String?>(null) }
    fun run(block: suspend () -> MediaCommandOutcome) {
        scope.launch {
            val outcome = block()
            message = if (outcome == MediaCommandOutcome.SENT) null else "That control isn't available for this player"
        }
    }

    CockpitCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .clip(AutoVerseShapes.small)
                    .background(colors.surfaceHighlight),
                contentAlignment = Alignment.Center,
            ) {
                val art = artwork
                if (art != null) {
                    Image(bitmap = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = colors.textFaint, modifier = Modifier.size(72.dp))
                }
            }
        }
        Spacer(Modifier.size(AutoVerseSpacing.sm))
        when (state) {
            MediaUiState.AccessRequired -> {
                Text("Music control is off", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text("Allow AutoVerse to control music to see and control songs from other apps.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Spacer(Modifier.size(AutoVerseSpacing.xs))
                PrimaryAction(text = "Allow", onClick = { openMediaAccessSettings(context) })
            }
            is MediaUiState.Active -> {
                Text(
                    active?.title ?: active?.appLabel ?: "Playing",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(state.artist, state.album).joinToString(" · ").ifBlank { state.appLabel ?: "" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "from ${state.appLabel ?: state.packageName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textFaint,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.packageName != context.packageName) {
                        TextAction(text = "Open app", onClick = { launchPackage(context, state.packageName) })
                    }
                }
                SeekBar(state = state, onSeek = { pos -> run { onSeek(pos) } })
            }
            else -> {
                Text("Nothing playing", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text("Pick a song on the right — from a USB drive, this unit or a music app.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BigControl(Icons.Outlined.SkipPrevious, "Previous", enabled = active != null) { run { onCommand(MediaCommand.PREVIOUS) } }
            BigControl(
                if (active?.status?.isPlaying == true) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                if (active?.status?.isPlaying == true) "Pause" else "Play",
                enabled = active != null,
                primary = true,
            ) { run { onCommand(mediaToggleCommand(active?.status)) } }
            BigControl(Icons.Outlined.SkipNext, "Next", enabled = active != null) { run { onCommand(MediaCommand.NEXT) } }
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = colors.warning, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SeekBar(state: MediaUiState.Active, onSeek: (Long) -> Unit) {
    val colors = LocalAutoVerseColors.current
    val progress = state.progress ?: return
    // Sessions report position only on changes; advance it locally while playing.
    val position by produceState(progress.positionMs, progress, state.status) {
        value = progress.positionMs
        while (state.status.isPlaying && value < progress.durationMs) {
            delay(1_000)
            value = (value + 1_000).coerceAtMost(progress.durationMs)
        }
    }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    Slider(
        value = if (dragging) dragValue else position.toFloat() / progress.durationMs,
        onValueChange = { dragging = true; dragValue = it },
        onValueChangeFinished = {
            dragging = false
            onSeek((dragValue * progress.durationMs).toLong())
        },
        colors = SliderDefaults.colors(thumbColor = colors.accent, activeTrackColor = colors.accent),
    )
    Row(Modifier.fillMaxWidth()) {
        Text(clock(position), style = MaterialTheme.typography.labelSmall, color = colors.textFaint, modifier = Modifier.weight(1f))
        Text(clock(progress.durationMs), style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
    }
}

@Composable
private fun BigControl(icon: ImageVector, label: String, enabled: Boolean, primary: Boolean = false, onClick: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Box(
        modifier = Modifier
            .size(if (primary) 76.dp else 60.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (primary) colors.accent else colors.surfaceHighlight)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = when {
                !enabled -> colors.textFaint
                primary -> colors.onAccent
                else -> colors.textPrimary
            },
            modifier = Modifier.size(if (primary) 44.dp else 34.dp),
        )
    }
}

// ---- right: queue, my music, apps -------------------------------------------

@Composable
private fun LibraryPanel(
    music: MusicController,
    mediaState: MediaUiState,
    onSkipToQueue: suspend (Long) -> MediaCommandOutcome,
    modifier: Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val apps by music.musicApps.collectAsStateWithLifecycle()
    val library by music.libraryState.collectAsStateWithLifecycle()
    val active = mediaState as? MediaUiState.Active
    val hasQueue = active?.queue?.isNotEmpty() == true
    var tab by rememberSaveable { mutableStateOf(TAB_LOCAL) }

    CockpitCard(modifier = modifier) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
            if (hasQueue) {
                item { TabChip("Up next", Icons.Outlined.QueueMusic, tab == TAB_QUEUE) { tab = TAB_QUEUE; music.closeApp() } }
            }
            item { TabChip("My music", Icons.Outlined.Usb, tab == TAB_LOCAL) { tab = TAB_LOCAL; music.closeApp() } }
            items(apps, key = { it.packageName }) { app ->
                TabChip(app.label, Icons.Outlined.LibraryMusic, tab == app.packageName) {
                    tab = app.packageName
                    music.openApp(app)
                }
            }
        }
        Spacer(Modifier.size(AutoVerseSpacing.sm))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                tab == TAB_QUEUE && active != null -> QueueList(active, onSkipToQueue)
                tab == TAB_LOCAL || apps.none { it.packageName == tab } -> LocalLibrary(music, library)
                else -> AppLibrary(music, apps.first { it.packageName == tab })
            }
        }
        if (apps.isEmpty() && tab == TAB_LOCAL) {
            Text(
                "No music apps found that share their library. Spotify, JioSaavn and others appear here when installed.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
            )
        }
    }
}

@Composable
private fun TabChip(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .clip(AutoVerseShapes.pill)
            .background(if (selected) colors.accent else colors.surfaceHighlight)
            .clickable(onClick = onClick)
            .heightIn(min = AutoVerseSpacing.touchMin)
            .padding(horizontal = AutoVerseSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) colors.onAccent else colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(AutoVerseSpacing.xs))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) colors.onAccent else colors.textPrimary, maxLines = 1)
    }
}

@Composable
private fun QueueList(active: MediaUiState.Active, onSkipToQueue: suspend (Long) -> MediaCommandOutcome) {
    val scope = rememberCoroutineScope()
    LazyColumn {
        itemsIndexed(active.queue, key = { _, e -> e.queueId }) { index, entry ->
            SongRow(
                title = mediaQueueEntryLabel(entry, index),
                subtitle = entry.subtitle,
                playing = entry.queueId == active.activeQueueId,
                onClick = { scope.launch { onSkipToQueue(entry.queueId) } },
            )
        }
    }
}

@Composable
private fun LocalLibrary(music: MusicController, library: com.autoverse.launcher.core.music.LibraryState) {
    val colors = LocalAutoVerseColors.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { music.refresh() }
    if (!library.permissionGranted) {
        CenteredMessage(
            icon = Icons.Outlined.Usb,
            title = "Play songs from USB, SD card or this unit",
            body = "Allow AutoVerse to read your music files. Only audio files are read; nothing leaves the unit.",
            action = "Allow" to { permission.launch(audioReadPermission()) },
        )
        return
    }
    if (library.loading && library.tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (library.tracks.isEmpty()) {
        CenteredMessage(
            icon = Icons.Outlined.Usb,
            title = "No songs found",
            body = "Plug in a USB drive or SD card with music. Songs appear here automatically within a few seconds.",
            action = "Check again" to music::refresh,
        )
        return
    }

    var view by rememberSaveable { mutableStateOf(LibraryView.SONGS) }
    var query by rememberSaveable { mutableStateOf("") }
    var openGroup by rememberSaveable { mutableStateOf<String?>(null) }
    val filtered = remember(library.tracks, query) { filterTracks(library.tracks, query) }
    val groups = remember(filtered, view) { groupTracks(filtered, view) }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
            AutoVerseTextField(
                value = query,
                onValueChange = { query = it; openGroup = null },
                placeholder = "Search ${library.tracks.size} songs",
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(text = "Shuffle all", onClick = { music.shuffleLocal(filtered) })
        }
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
            LibraryView.entries.forEach { v ->
                TextAction(
                    text = v.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { view = v; openGroup = null },
                    enabled = view != v,
                )
            }
        }
        val group = openGroup
        if (view == LibraryView.SONGS || group != null) {
            val tracks = if (view == LibraryView.SONGS) filtered else groups.firstOrNull { it.first == group }?.second.orEmpty()
            if (group != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp).clickable { openGroup = null },
                    )
                    Spacer(Modifier.width(AutoVerseSpacing.xs))
                    Text(group, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    TextAction(text = "Play all", onClick = { music.playLocal(tracks, 0) })
                }
            }
            TrackList(tracks) { index -> music.playLocal(tracks, index) }
        } else {
            LazyColumn {
                items(groups, key = { it.first }) { (name, tracks) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openGroup = name }
                            .heightIn(min = AutoVerseSpacing.touchComfort)
                            .padding(horizontal = AutoVerseSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (view == LibraryView.FOLDERS) Icons.Outlined.Folder else Icons.Outlined.Album,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(26.dp),
                        )
                        Spacer(Modifier.width(AutoVerseSpacing.sm))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${tracks.size} song${if (tracks.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackList(tracks: List<LocalTrack>, onPlay: (Int) -> Unit) {
    LazyColumn {
        itemsIndexed(tracks, key = { _, t -> t.uri }) { index, t ->
            SongRow(
                title = t.title,
                subtitle = "${t.artist} · ${t.source}",
                trailing = clock(t.durationMs),
                onClick = { onPlay(index) },
            )
        }
    }
}

@Composable
private fun AppLibrary(music: MusicController, app: MusicApp) {
    val colors = LocalAutoVerseColors.current
    val context = LocalContext.current
    val state by music.browseState.collectAsStateWithLifecycle()
    var note by remember(app) { mutableStateOf<String?>(null) }

    when (val s = state) {
        is AppBrowseState.Connecting, AppBrowseState.None ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

        is AppBrowseState.Refused -> CenteredMessage(
            icon = Icons.Outlined.LibraryMusic,
            title = "${app.label} keeps its library private",
            body = "${app.label} doesn't let other apps list its songs. Play, pause and skip still work from AutoVerse once it's playing.",
            action = "Open ${app.label}" to { launchPackage(context, app.packageName) },
        )

        is AppBrowseState.Listing -> Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (s.path.size > 1) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp).clickable { music.browseBack() },
                    )
                    Spacer(Modifier.width(AutoVerseSpacing.xs))
                }
                Text(s.path.last().first, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextAction(text = "Resume", onClick = {
                    note = if (music.resumeApp()) null else "${app.label} didn't respond — open it once, then try again"
                })
                Icon(
                    Icons.Outlined.OpenInNew,
                    contentDescription = "Open ${app.label}",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(26.dp).clickable { launchPackage(context, app.packageName) },
                )
            }
            note?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = colors.warning) }
            val items = s.items
            when {
                items == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                items.isEmpty() -> Text(
                    "Nothing to show here. ${app.label} may need you to sign in or open it once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textFaint,
                    modifier = Modifier.padding(AutoVerseSpacing.sm),
                )
                else -> LazyColumn {
                    items(items, key = { it.mediaId + it.title }) { item ->
                        SongRow(
                            title = item.title,
                            subtitle = item.subtitle,
                            icon = if (item.browsable) Icons.Outlined.Folder else Icons.Outlined.MusicNote,
                            onClick = {
                                if (item.browsable) {
                                    music.openFolder(item)
                                } else if (item.playable) {
                                    note = if (music.playFromApp(item)) null else "${app.label} didn't start that song"
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    playing: Boolean = false,
    trailing: String? = null,
    icon: ImageVector = Icons.Outlined.MusicNote,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.tile)
            .background(if (playing) colors.surfaceHighlight else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = AutoVerseSpacing.touchComfort)
            .padding(horizontal = AutoVerseSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (playing) colors.accent else colors.textFaint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(AutoVerseSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (playing) colors.accent else colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = colors.textFaint)
        }
    }
}

@Composable
private fun CenteredMessage(icon: ImageVector, title: String, body: String, action: Pair<String, () -> Unit>?) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(AutoVerseSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(44.dp))
        Spacer(Modifier.size(AutoVerseSpacing.sm))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.size(AutoVerseSpacing.xxs))
        Text(body, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
        if (action != null) {
            Spacer(Modifier.size(AutoVerseSpacing.md))
            PrimaryAction(text = action.first, onClick = action.second)
        }
    }
}

private fun clock(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}

private fun launchPackage(context: Context, packageName: String) {
    try {
        context.packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ?.let(context::startActivity)
    } catch (_: Exception) {
    }
}
