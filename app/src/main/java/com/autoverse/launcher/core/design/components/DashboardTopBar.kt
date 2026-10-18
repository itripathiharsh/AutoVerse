package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.connectivity.ConnectivityStatus
import com.autoverse.launcher.core.connectivity.LinkState
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.StatusChipsMode
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.performance.LocalPerformanceSpec

/**
 * Phase 15 — shared top strip for the Home dashboard and the app drawer.
 *
 * Extracted from the Home header so both screens render the identical live
 * clock/date and connectivity treatment instead of drifting copies. Callers
 * choose the composition: Home shows the brand plate outside its rail
 * archetype and discloses the FPS chip per policy; the drawer always shows
 * the logo-free variant and passes [onBack] in its compact form (where no
 * rail exists) and null where the rail carries Home.
 *
 * Connectivity icons hide entirely while a link state is UNKNOWN: the strip
 * only ever shows data the platform actually reported.
 */
@Composable
fun DashboardTopBar(
    clockText: String,
    dateText: String,
    modifier: Modifier = Modifier,
    connectivity: ConnectivityStatus = ConnectivityStatus(),
    chips: StatusChipsMode = StatusChipsMode.NONE,
    fpsText: String? = null,
    showLogo: Boolean = false,
    onBack: (() -> Unit)? = null,
    onOpenDiagnostics: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textSecondary,
                )
            }
        }
        if (showLogo) {
            BrandPlate(logoHeight = 32.dp)
            Spacer(Modifier.size(AutoVerseSpacing.xs))
        }
        LinkIndicator(
            icon = Icons.Outlined.Wifi,
            state = connectivity.wifi,
            contentDescription = wifiDescription(connectivity.wifi),
        )
        LinkIndicator(
            icon = Icons.Outlined.Bluetooth,
            state = connectivity.bluetooth,
            contentDescription = bluetoothDescription(connectivity.bluetooth),
        )
        Spacer(Modifier.weight(1f))
        if (chips != StatusChipsMode.NONE && spec.showFpsReadout && fpsText != null) {
            StatusChip(label = fpsText, leadingDot = true, tone = StatusTone.SUCCESS)
        }
        ClockBlock(
            clockText = clockText,
            dateText = dateText,
            compact = chips == StatusChipsMode.NONE,
        )
        if (onOpenDiagnostics != null) {
            IconButton(onClick = onOpenDiagnostics) {
                Icon(
                    Icons.Outlined.Memory,
                    contentDescription = "Diagnostics",
                    tint = colors.textSecondary,
                )
            }
        }
        if (onOpenSettings != null) {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = colors.textSecondary,
                )
            }
        }
    }
}

/**
 * Connectivity indicator. Rendered only when the platform returned a
 * reliable answer — UNKNOWN hides the icon entirely rather than guessing.
 */
@Composable
private fun LinkIndicator(
    icon: ImageVector,
    state: LinkState,
    contentDescription: String,
) {
    if (state == LinkState.UNKNOWN) return
    val colors = LocalAutoVerseColors.current
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(AutoVerseIcons.compact),
        tint = if (state == LinkState.CONNECTED) colors.textSecondary else colors.textFaint,
    )
}

private fun wifiDescription(state: LinkState): String =
    if (state == LinkState.CONNECTED) "Wi-Fi connected" else "Wi-Fi not connected"

private fun bluetoothDescription(state: LinkState): String =
    if (state == LinkState.CONNECTED) "Bluetooth on" else "Bluetooth off"

@Composable
private fun ClockBlock(clockText: String, dateText: String, compact: Boolean) {
    val colors = LocalAutoVerseColors.current
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = clockText,
            style = if (compact) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.displayMedium
            },
            color = colors.textPrimary,
            maxLines = 1,
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
