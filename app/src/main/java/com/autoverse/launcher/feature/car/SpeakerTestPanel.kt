package com.autoverse.launcher.feature.car

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.core.car.CarController
import com.autoverse.launcher.core.car.SpeakerLayout
import com.autoverse.launcher.core.car.SpeakerPosition
import com.autoverse.launcher.core.car.SpeakerSoundSettings
import com.autoverse.launcher.core.car.SpeakerTone
import com.autoverse.launcher.core.car.speakerRoute
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

private const val FADER_HINT =
    "This unit mixes to 2 channels. To test the rear speakers, move the Fader to Rear in the unit's sound settings, then tap again."

/** Car-shaped outline for the cabin diagram: rounder at the bonnet than the boot. */
private val CabinShape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomStart = 24.dp, bottomEnd = 24.dp)

/**
 * Speaker test: pick how many speakers the car has, tap one on the cabin
 * diagram to hear it alone, or walk them all in turn. Stops any tone when
 * the section is left or the app goes to the background.
 */
@Composable
internal fun SpeakerTestPanel(car: CarController) {
    val colors = LocalAutoVerseColors.current
    val context = LocalContext.current
    val layout by car.speakerLayout.collectAsStateWithLifecycle()
    val outputInfo by car.speakerOutput.collectAsStateWithLifecycle()
    val playingSpeaker by car.playingSpeaker.collectAsStateWithLifecycle()
    val playingTone by car.playingTone.collectAsStateWithLifecycle()
    val walking by car.walkingSpeakers.collectAsStateWithLifecycle()
    val lastSpeaker by car.lastSpeaker.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { car.refreshSpeakerOutput() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) car.stopTone()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            car.stopTone()
        }
    }

    val anythingPlaying = playingSpeaker != null || playingTone != null || walking
    val info = outputInfo
    val showFaderHint = info != null && layout.hasRear &&
        lastSpeaker?.let { speakerRoute(it, info.output).needsFader } == true
    val onSpeakerTap: (SpeakerPosition) -> Unit = { position ->
        if (playingSpeaker == position && !walking) car.stopTone() else car.playSpeaker(position)
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Speaker test", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Text(
                    "Set the volume to about a third, then tap a speaker to hear it on its own.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            if (anythingPlaying) TextAction(text = "Stop", onClick = car::stopTone, icon = Icons.Outlined.Stop, quiet = true)
        }
        Spacer(Modifier.height(AutoVerseSpacing.md))
        Text("Speakers in your car", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        LayoutPicker(selected = layout, onPick = car::setSpeakerLayout)
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        Text(
            info?.summary ?: "Checking the unit's audio output…",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textFaint,
        )
        Spacer(Modifier.height(AutoVerseSpacing.md))

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 560.dp
            val panelWidth = maxWidth
            val diagram: @Composable (Modifier) -> Unit = { m ->
                CabinDiagram(layout = layout, playing = playingSpeaker, onTap = onSpeakerTap, modifier = m)
            }
            val controls: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
                    PrimaryAction(
                        text = if (walking) "Stop" else "Play all in turn",
                        onClick = { if (walking) car.stopTone() else car.walkSpeakers() },
                        icon = if (walking) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val current = playingSpeaker
                    if (walking && current != null) {
                        val step = layout.walkOrder.indexOf(current) + 1
                        Text(
                            "Now playing: ${current.label} ($step of ${layout.walkOrder.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                        )
                    }
                    if (showFaderHint) FaderHint(onOpenSettings = { SpeakerSoundSettings.open(context) })
                    Text("More tests", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    val bassActive = playingSpeaker == SpeakerPosition.SUBWOOFER && !walking
                    ExtraTile(
                        label = "Bass",
                        detail = "Low 50 Hz tone — checks the subwoofer and door rattles",
                        active = bassActive,
                        onClick = { if (bassActive) car.stopTone() else car.playSpeaker(SpeakerPosition.SUBWOOFER) },
                    )
                    listOf(SpeakerTone.SWEEP, SpeakerTone.PHASE).forEach { tone ->
                        val active = playingTone == tone
                        ExtraTile(
                            label = tone.label,
                            detail = tone.detail,
                            active = active,
                            onClick = { if (active) car.stopTone() else car.playTone(tone) },
                        )
                    }
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap)) {
                    diagram(Modifier.width(minOf(panelWidth * 0.5f, 360.dp)))
                    controls(Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md)) {
                    diagram(Modifier.align(Alignment.CenterHorizontally).widthIn(max = 420.dp).fillMaxWidth())
                    controls(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun LayoutPicker(selected: SpeakerLayout, onPick: (SpeakerLayout) -> Unit) {
    val colors = LocalAutoVerseColors.current
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        SpeakerLayout.entries.forEach { layout ->
            val isSelected = layout == selected
            Box(
                Modifier
                    .clip(AutoVerseShapes.pill)
                    .background(if (isSelected) colors.accentSoft else colors.surfaceHighlight)
                    .then(
                        if (isSelected) Modifier.border(AutoVerseBorders.hairline, colors.accent, AutoVerseShapes.pill) else Modifier,
                    )
                    .clickable(role = Role.RadioButton) { onPick(layout) }
                    .heightIn(min = AutoVerseSpacing.touchMin)
                    .padding(horizontal = AutoVerseSpacing.chipHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    layout.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Top-down cabin: dash tweeters, front doors, rear doors, subwoofer in the boot. */
@Composable
private fun CabinDiagram(
    layout: SpeakerLayout,
    playing: SpeakerPosition?,
    onTap: (SpeakerPosition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier
            .clip(CabinShape)
            .background(colors.surfaceRaised)
            .border(AutoVerseBorders.hairline, colors.outlineStrong, CabinShape)
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("FRONT", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
        if (layout.hasDash) {
            CabinRow {
                SpeakerTile(SpeakerPosition.DASH_LEFT, playing, onTap, Modifier.weight(1f))
                Spacer(Modifier.weight(0.6f))
                SpeakerTile(SpeakerPosition.DASH_RIGHT, playing, onTap, Modifier.weight(1f))
            }
        }
        CabinRow {
            SpeakerTile(SpeakerPosition.FRONT_LEFT, playing, onTap, Modifier.weight(1f))
            Seats(Modifier.weight(0.6f))
            SpeakerTile(SpeakerPosition.FRONT_RIGHT, playing, onTap, Modifier.weight(1f))
        }
        if (layout.hasRear) {
            CabinRow {
                SpeakerTile(SpeakerPosition.REAR_LEFT, playing, onTap, Modifier.weight(1f))
                Seats(Modifier.weight(0.6f))
                SpeakerTile(SpeakerPosition.REAR_RIGHT, playing, onTap, Modifier.weight(1f))
            }
        }
        if (layout.hasSub) {
            CabinRow {
                Spacer(Modifier.weight(0.5f))
                SpeakerTile(SpeakerPosition.SUBWOOFER, playing, onTap, Modifier.weight(1.6f))
                Spacer(Modifier.weight(0.5f))
            }
            Text("BOOT", style = MaterialTheme.typography.labelSmall, color = colors.textFaint)
        }
    }
}

@Composable
private fun CabinRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Two faint seat shapes between the door speakers, so the diagram reads as a cabin. */
@Composable
private fun Seats(modifier: Modifier) {
    val colors = LocalAutoVerseColors.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs, Alignment.CenterHorizontally)) {
        repeat(2) {
            Box(
                Modifier
                    .size(width = 18.dp, height = 28.dp)
                    .clip(AutoVerseShapes.small)
                    .background(colors.outline),
            )
        }
    }
}

@Composable
private fun SpeakerTile(
    position: SpeakerPosition,
    playing: SpeakerPosition?,
    onTap: (SpeakerPosition) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val active = playing == position
    val content = if (active) colors.onAccent else colors.textPrimary
    Column(
        modifier
            .clip(AutoVerseShapes.small)
            .background(if (active) colors.accent else colors.surfaceHighlight)
            .clickable(role = Role.Button, onClickLabel = "Play ${position.label}") { onTap(position) }
            .heightIn(min = AutoVerseSpacing.touchComfort)
            .padding(horizontal = AutoVerseSpacing.xs, vertical = AutoVerseSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            if (active) Icons.Outlined.GraphicEq else Icons.Outlined.Speaker,
            contentDescription = null,
            tint = if (active) colors.onAccent else colors.accent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            position.shortLabel,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FaderHint(onOpenSettings: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.small)
            .background(colors.surfaceHighlight)
            .padding(AutoVerseSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.warning, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(AutoVerseSpacing.xs))
            Text(FADER_HINT, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        }
        TextAction(text = "Open sound settings", onClick = onOpenSettings, icon = Icons.Outlined.Tune)
    }
}

@Composable
private fun ExtraTile(label: String, detail: String, active: Boolean, onClick: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.small)
            .background(if (active) colors.accent else colors.surfaceHighlight)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = AutoVerseSpacing.touchComfort)
            .padding(horizontal = AutoVerseSpacing.md, vertical = AutoVerseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (active) Icons.Outlined.Stop else Icons.Outlined.PlayArrow,
            contentDescription = null,
            tint = if (active) colors.onAccent else colors.accent,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(AutoVerseSpacing.sm))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = if (active) colors.onAccent else colors.textPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (active) colors.onAccent else colors.textSecondary)
        }
    }
}
