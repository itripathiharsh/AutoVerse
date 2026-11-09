package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material.icons.outlined.NorthWest
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TurnLeft
import androidx.compose.material.icons.outlined.TurnRight
import androidx.compose.material.icons.outlined.TurnSlightLeft
import androidx.compose.material.icons.outlined.TurnSlightRight
import androidx.compose.material.icons.outlined.UTurnLeft
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.routing.InAppNavigationController
import com.autoverse.launcher.core.routing.ManeuverType
import com.autoverse.launcher.core.routing.NavPlace
import com.autoverse.launcher.core.routing.NavState
import com.autoverse.launcher.core.routing.NavText
import com.autoverse.launcher.core.routing.LocalNavLanguage
import com.autoverse.launcher.core.routing.formatDistance
import com.autoverse.launcher.core.routing.formatDuration
import com.autoverse.launcher.core.routing.maneuverText
import org.maplibre.android.geometry.LatLng
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Keeps the live map in step with navigation: draws/clears the route line,
 * frames the whole route for the preview, and switches to the heading-up
 * driving camera when guidance starts.
 */
@Composable
fun InAppNavigationMapSync(controller: InAppNavigationController) {
    val host = LocalLiveMapHost.current ?: return
    val state by controller.state.collectAsStateWithLifecycle()
    val padding = with(LocalDensity.current) { 40.dp.roundToPx() }
    val bottomPadding = with(LocalDensity.current) { 190.dp.roundToPx() } // clear of the preview card
    val route = when (val s = state) {
        is NavState.Preview -> s.route
        is NavState.Guiding -> s.route
        else -> null
    }
    LaunchedEffect(host, route) {
        host.setRoute(route?.points?.map { LatLng(it.latitude, it.longitude) })
    }
    val mode = when (state) {
        is NavState.Preview -> 1
        is NavState.Guiding -> 2
        is NavState.Idle -> 0
        else -> -1 // preparing / arrived / failed: leave the camera where it is
    }
    var previousMode by remember { mutableIntStateOf(0) }
    LaunchedEffect(host, mode, route) {
        // The map may still be resizing (keyboard closing); let layout settle first.
        delay(350)
        when (mode) {
            1 -> route?.let { r ->
                host.showOverview(r.points.map { LatLng(it.latitude, it.longitude) }, padding, bottomPadding)
            }
            2 -> host.followForGuidance()
            0 -> if (previousMode != 0) host.followCar()
        }
        // Remember the last real camera mode, so Arrived/Failed → Idle still recenters.
        if (mode != -1) previousMode = mode
    }
}

/** Everything drawn on top of the map for in-app navigation. */
@Composable
fun BoxScope.InAppNavigationOverlay(
    controller: InAppNavigationController,
    onOpenInGoogleMaps: (NavPlace) -> Unit,
    compact: Boolean,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val search by controller.searchState.collectAsStateWithLifecycle()
    val voiceOn by controller.voiceOn.collectAsStateWithLifecycle()
    val lang = LocalNavLanguage.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    when (val s = state) {
        NavState.Idle -> if (search.query.isNotBlank()) {
            SearchResults(
                results = search.results,
                loading = search.loading,
                error = search.error,
                onPick = { place ->
                    // Close the keyboard so the map gets its full size back.
                    keyboard?.hide()
                    focus.clearFocus()
                    controller.navigateTo(place)
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AutoVerseSpacing.xs),
            )
        }

        is NavState.Preparing -> OverlayCard(Modifier.align(Alignment.Center)) {
            Text(s.place.name, style = MaterialTheme.typography.titleMedium, color = LocalAutoVerseColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            val d = s.download
            if (d != null) {
                Text(
                    "Downloading road data for this area (first time only) · ${d.tileIndex + 1}/${d.tileCount}" +
                        if (d.bytesTotal > 0) " · ${d.bytesDone / (1024 * 1024)}/${d.bytesTotal / (1024 * 1024)} MB" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalAutoVerseColors.current.textSecondary,
                )
                Spacer(Modifier.size(AutoVerseSpacing.xs))
                LinearProgressIndicator(progress = { d.fraction }, modifier = Modifier.fillMaxWidth())
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(AutoVerseSpacing.xs))
                    Text(NavText.findingRoute(lang), style = MaterialTheme.typography.bodySmall, color = LocalAutoVerseColors.current.textSecondary)
                }
            }
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            TextAction(text = NavText.cancel(lang), onClick = controller::stop)
        }

        is NavState.Preview -> OverlayCard(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(AutoVerseSpacing.xs),
        ) {
            val colors = LocalAutoVerseColors.current
            Text(s.place.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (s.place.detail.isNotBlank()) {
                Text(s.place.detail, style = MaterialTheme.typography.bodySmall, color = colors.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.size(AutoVerseSpacing.xxs))
            Text(
                "${formatDuration(s.route.totalSeconds)} · ${formatDistance(s.route.totalMeters)} · ${NavText.arrive(lang)} ${arrivalClock(s.route.totalSeconds)}",
                style = MaterialTheme.typography.titleSmall,
                color = colors.success,
            )
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryAction(text = NavText.start(lang), onClick = controller::startGuidance)
                TextAction(text = NavText.cancel(lang), onClick = controller::stop)
                if (!compact) TextAction(text = "Google Maps", onClick = { onOpenInGoogleMaps(s.place) })
            }
        }

        is NavState.Guiding -> {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(AutoVerseSpacing.sm),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
            ) {
                s.progress?.speedLimitKmh?.let { com.autoverse.launcher.feature.home.SpeedLimitSign(it, size = if (compact) 48 else 60) }
                s.progress?.nextCameraMeters?.takeIf { it < 1_500 }?.let { d ->
                    Text(
                        "📷 ${formatDistance(d)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .clip(AutoVerseShapes.pill)
                            .background(androidx.compose.ui.graphics.Color(0xFFD32F2F))
                            .padding(horizontal = AutoVerseSpacing.sm, vertical = 4.dp),
                    )
                }
            }
            GuidanceBanner(
                state = s,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(AutoVerseSpacing.xs),
                compact = compact,
            )
            GuidanceFooter(
                state = s,
                voiceOn = voiceOn,
                onToggleVoice = controller::toggleVoice,
                onEnd = controller::stop,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(AutoVerseSpacing.xs),
            )
        }

        is NavState.Arrived -> OverlayCard(Modifier.align(Alignment.Center)) {
            val colors = LocalAutoVerseColors.current
            Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.success, modifier = Modifier.size(32.dp))
            Text(NavText.arrivedTitle(lang), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text(s.place.name, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            PrimaryAction(text = NavText.done(lang), onClick = controller::stop)
        }

        is NavState.Failed -> OverlayCard(Modifier.align(Alignment.Center)) {
            val colors = LocalAutoVerseColors.current
            Text(s.place.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(s.message, style = MaterialTheme.typography.bodySmall, color = colors.warning)
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs), verticalAlignment = Alignment.CenterVertically) {
                PrimaryAction(text = NavText.retry(lang), onClick = controller::retry)
                TextAction(text = NavText.cancel(lang), onClick = controller::stop)
                if (!compact) TextAction(text = "Google Maps", onClick = { onOpenInGoogleMaps(s.place) })
            }
        }
    }
}

/** The "Where to?" row under the map; hidden while guiding so the map gets the room. */
@Composable
fun InAppNavigationSearchRow(
    controller: InAppNavigationController,
    onOpenGoogleMaps: () -> Unit,
    showRecents: Boolean,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val search by controller.searchState.collectAsStateWithLifecycle()
    val recents by controller.recents.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    if (state is NavState.Guiding || state is NavState.Preview) return

    Spacer(Modifier.size(AutoVerseSpacing.xs))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        DestinationField(
            value = search.query,
            onValueChange = controller::setQuery,
            isError = false,
            placeholder = NavText.whereTo(LocalNavLanguage.current),
            modifier = Modifier.weight(1f),
            trailingIcon = if (search.query.isNotEmpty()) {
                {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Clear",
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = controller::clearSearch),
                    )
                }
            } else null,
        )
        TextAction(text = "Google Maps", onClick = onOpenGoogleMaps)
    }
    if (showRecents && search.query.isBlank() && recents.isNotEmpty() && state is NavState.Idle) {
        val colors = LocalAutoVerseColors.current
        LazyRow(
            modifier = Modifier.padding(top = AutoVerseSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
        ) {
            items(recents.take(6), key = { it.name + it.detail + it.latitude }) { place ->
                Row(
                    modifier = Modifier
                        .clip(AutoVerseShapes.pill)
                        .background(colors.surfaceHighlight)
                        .clickable {
                            keyboard?.hide()
                            focus.clearFocus()
                            controller.navigateTo(place)
                        }
                        .heightIn(min = AutoVerseSpacing.touchMin)
                        .padding(horizontal = AutoVerseSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
                ) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textFaint)
                    Text(
                        place.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 180.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResults(
    results: List<NavPlace>,
    loading: Boolean,
    error: String?,
    onPick: (NavPlace) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.small)
            .background(colors.surface.copy(alpha = 0.97f))
            .padding(vertical = AutoVerseSpacing.xxs),
    ) {
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = AutoVerseSpacing.sm))
        }
        when {
            error != null && results.isEmpty() -> Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = colors.warning,
                modifier = Modifier.padding(AutoVerseSpacing.sm),
            )
            results.isEmpty() && !loading -> Text(
                "Type at least 3 letters — a place, area or address",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
                modifier = Modifier.padding(AutoVerseSpacing.sm),
            )
        }
        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
            items(results, key = { it.name + it.detail + it.latitude + it.longitude }) { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(place) }
                        .heightIn(min = AutoVerseSpacing.touchMin)
                        .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(AutoVerseSpacing.sm))
                    Column(Modifier.weight(1f)) {
                        Text(place.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (place.detail.isNotBlank()) {
                            Text(place.detail, style = MaterialTheme.typography.bodySmall, color = colors.textFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidanceBanner(state: NavState.Guiding, modifier: Modifier, compact: Boolean) {
    val lang = LocalNavLanguage.current
    val colors = LocalAutoVerseColors.current
    val progress = state.progress
    val next = progress?.nextManeuver
    Row(
        modifier = modifier
            .widthIn(max = 520.dp)
            .clip(AutoVerseShapes.small)
            .background(colors.accent)
            .padding(horizontal = AutoVerseSpacing.md, vertical = AutoVerseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = next?.type?.let(::maneuverIcon) ?: Icons.Outlined.ArrowUpward,
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(if (compact) 36.dp else 48.dp),
        )
        Spacer(Modifier.width(AutoVerseSpacing.sm))
        Column {
            when {
                state.rerouting -> Text(NavText.recalculatingTitle(lang), color = colors.onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                progress == null || next == null -> Text("Head to the route", color = colors.onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                else -> {
                    Text(
                        formatDistance(progress.distanceToNextManeuver),
                        color = colors.onAccent,
                        fontSize = if (compact) 24.sp else 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(NavText.maneuver(next, lang), color = colors.onAccent, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    progress.thenManeuver?.let {
                        Text(NavText.then(it, lang), color = colors.onAccent.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidanceFooter(
    state: NavState.Guiding,
    voiceOn: Boolean,
    onToggleVoice: () -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val remainingS = state.progress?.remainingSeconds ?: state.route.totalSeconds
    val remainingM = state.progress?.remainingMeters ?: state.route.totalMeters
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.small)
            .background(colors.surface.copy(alpha = 0.95f))
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                arrivalClock(remainingS),
                style = MaterialTheme.typography.titleMedium,
                color = colors.success,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "${formatDuration(remainingS)} · ${formatDistance(remainingM)}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
        TransportButton(
            icon = if (voiceOn) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
            contentDescription = if (voiceOn) "Mute voice" else "Unmute voice",
            onClick = onToggleVoice,
        )
        Spacer(Modifier.width(AutoVerseSpacing.xs))
        PrimaryAction(text = NavText.end(LocalNavLanguage.current), onClick = onEnd)
    }
}

@Composable
private fun OverlayCard(modifier: Modifier, content: @Composable () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = modifier
            .padding(AutoVerseSpacing.sm)
            .widthIn(max = 460.dp)
            .clip(AutoVerseShapes.small)
            .background(colors.surface.copy(alpha = 0.96f))
            .padding(AutoVerseSpacing.md),
    ) { content() }
}

private fun maneuverIcon(type: ManeuverType): ImageVector = when (type) {
    ManeuverType.TURN_LEFT, ManeuverType.TURN_SHARP_LEFT -> Icons.Outlined.TurnLeft
    ManeuverType.TURN_RIGHT, ManeuverType.TURN_SHARP_RIGHT -> Icons.Outlined.TurnRight
    ManeuverType.TURN_SLIGHT_LEFT -> Icons.Outlined.TurnSlightLeft
    ManeuverType.TURN_SLIGHT_RIGHT -> Icons.Outlined.TurnSlightRight
    ManeuverType.KEEP_LEFT, ManeuverType.EXIT_LEFT -> Icons.Outlined.NorthWest
    ManeuverType.KEEP_RIGHT, ManeuverType.EXIT_RIGHT -> Icons.Outlined.NorthEast
    ManeuverType.U_TURN -> Icons.Outlined.UTurnLeft
    ManeuverType.ROUNDABOUT -> Icons.Outlined.Refresh
    ManeuverType.ARRIVE -> Icons.Outlined.Place
    ManeuverType.CONTINUE -> Icons.Outlined.ArrowUpward
}

/** Whether clocks use 24-hour time; provided from the app's Clock setting at the root. */
val LocalUse24HourClock = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
private fun arrivalClock(remainingSeconds: Double): String =
    LocalTime.now().plusSeconds(remainingSeconds.toLong())
        .format(DateTimeFormatter.ofPattern(if (LocalUse24HourClock.current) "HH:mm" else "h:mm a"))
