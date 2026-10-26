package com.autoverse.launcher.feature.obd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.StatusChip
import com.autoverse.launcher.core.design.components.StatusTone
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.obd.ObdAdapter
import com.autoverse.launcher.core.obd.ObdConnectionState
import com.autoverse.launcher.core.obd.ObdDtcState
import com.autoverse.launcher.core.obd.ObdSample
import com.autoverse.launcher.core.obd.ObdTroubleCode

@Composable
fun ObdDiagnosticsScreen(
    obdState: ObdConnectionState,
    pairedAdapters: List<ObdAdapter>,
    dtcState: ObdDtcState,
    onRefreshAdapters: () -> Unit,
    onConnectObd: (deviceAddress: String?) -> Unit,
    onDisconnectObd: () -> Unit,
    onRequestDtcs: () -> Unit,
    onRequestBluetoothPermission: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenSpeedometer: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    var selectedAdapterAddress by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        onRefreshAdapters()
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
                selected = RailDestination.OBD,
                onOpenHome = onOpenHome,
                onOpenAppDrawer = onOpenAppDrawer,
                onOpenNavigation = onOpenNavigation,
                onOpenSettings = onOpenSettings,
                onOpenSpeedometer = onOpenSpeedometer,
                onOpenObd = {},
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
                // Left Column: Adapter Connection & Trouble Codes (weight 1f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap),
                ) {
                    ObdConnectionCard(
                        obdState = obdState,
                        pairedAdapters = pairedAdapters,
                        selectedAddress = selectedAdapterAddress,
                        onSelectAddress = { selectedAdapterAddress = it },
                        onRefreshAdapters = onRefreshAdapters,
                        onConnect = { onConnectObd(selectedAdapterAddress) },
                        onDisconnect = onDisconnectObd,
                        onRequestPermission = onRequestBluetoothPermission,
                        modifier = Modifier.weight(1.15f),
                    )

                    ObdDtcCard(
                        obdState = obdState,
                        dtcState = dtcState,
                        onRequestDtcs = onRequestDtcs,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Right Column: Live ECU Telemetry (weight 1.35f)
                ObdTelemetryCard(
                    obdState = obdState,
                    modifier = Modifier
                        .weight(1.35f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun ObdConnectionCard(
    obdState: ObdConnectionState,
    pairedAdapters: List<ObdAdapter>,
    selectedAddress: String?,
    onSelectAddress: (String) -> Unit,
    onRefreshAdapters: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current

    WidgetContainer(
        title = "ADAPTER CONNECTION",
        icon = Icons.Outlined.Bluetooth,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AutoVerseSpacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Status Chip & Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (obdState) {
                    is ObdConnectionState.Connected -> StatusChip(
                        icon = Icons.Outlined.BluetoothConnected,
                        label = "CONNECTED",
                        tone = StatusTone.SUCCESS,
                    )
                    is ObdConnectionState.Connecting -> StatusChip(
                        icon = Icons.Outlined.Bluetooth,
                        label = "CONNECTING...",
                        tone = StatusTone.WARNING,
                    )
                    is ObdConnectionState.PermissionRequired -> StatusChip(
                        icon = Icons.Outlined.BluetoothDisabled,
                        label = "PERMISSION NEEDED",
                        tone = StatusTone.DANGER,
                    )
                    is ObdConnectionState.Error -> StatusChip(
                        icon = Icons.Outlined.Warning,
                        label = "ERROR",
                        tone = StatusTone.DANGER,
                    )
                    is ObdConnectionState.Disconnected -> StatusChip(
                        icon = Icons.Outlined.Bluetooth,
                        label = "DISCONNECTED",
                        tone = StatusTone.NEUTRAL,
                    )
                }

                TextAction(
                    text = "Scan/Refresh",
                    onClick = onRefreshAdapters,
                )
            }

            // Adapter Selection List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = AutoVerseSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs),
            ) {
                if (obdState is ObdConnectionState.Connected) {
                    Text(
                        text = "Active: ${obdState.deviceName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent,
                    )
                    Text(
                        text = "Protocol: ELM327 RFCOMM (SPP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                } else if (pairedAdapters.isEmpty()) {
                    Text(
                        text = "No bonded Bluetooth adapters found.\nPair your ELM327 adapter in Android Bluetooth settings first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textFaint,
                    )
                } else {
                    pairedAdapters.forEach { adapter ->
                        val isSelected = adapter.address == selectedAddress ||
                            (selectedAddress == null && adapter == pairedAdapters.firstOrNull())
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AutoVerseShapes.tile)
                                .background(if (isSelected) colors.surfaceHighlight else Color.Transparent)
                                .clickable { onSelectAddress(adapter.address) }
                                .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xs),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(
                                        text = adapter.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) colors.textPrimary else colors.textSecondary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                    Text(
                                        text = adapter.address,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textFaint,
                                    )
                                }
                                if (adapter.likelyObd) {
                                    StatusChip(
                                        label = "OBD",
                                        tone = StatusTone.NEUTRAL,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Connect / Disconnect Action
            when (obdState) {
                is ObdConnectionState.Connected -> PrimaryAction(
                    text = "Disconnect Adapter",
                    onClick = onDisconnect,
                )
                is ObdConnectionState.Connecting -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = colors.accent,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                }
                is ObdConnectionState.PermissionRequired -> PrimaryAction(
                    text = "Grant Bluetooth Access",
                    onClick = onRequestPermission,
                )
                else -> PrimaryAction(
                    text = "Connect Adapter",
                    onClick = onConnect,
                    enabled = pairedAdapters.isNotEmpty(),
                )
            }
        }
    }
}

@Composable
private fun ObdDtcCard(
    obdState: ObdConnectionState,
    dtcState: ObdDtcState,
    onRequestDtcs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current

    WidgetContainer(
        title = "FAULT CODES (MODE 03)",
        icon = Icons.Outlined.Warning,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AutoVerseSpacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                when (dtcState) {
                    is ObdDtcState.Idle -> {
                        Text(
                            text = "Tap 'Read DTCs' to query stored diagnostic trouble codes from the vehicle ECU.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textFaint,
                        )
                    }
                    is ObdDtcState.Reading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
                            modifier = Modifier.padding(vertical = AutoVerseSpacing.sm),
                        ) {
                            CircularProgressIndicator(
                                color = colors.accent,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Text(
                                text = "Reading Mode 03 codes...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                            )
                        }
                    }
                    is ObdDtcState.Codes -> {
                        if (dtcState.codes.isEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.success,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = "No fault codes stored (ECU clear)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.success,
                                )
                            }
                        } else {
                            dtcState.codes.forEach { dtc ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(AutoVerseShapes.tile)
                                        .background(colors.surfaceHighlight)
                                        .padding(AutoVerseSpacing.sm),
                                ) {
                                    Column {
                                        Text(
                                            text = dtc.code,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.danger,
                                        )
                                        if (dtc.description != null) {
                                            Text(
                                                text = dtc.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colors.textSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    is ObdDtcState.Unavailable -> {
                        Text(
                            text = "Read unavailable: ${dtcState.reason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.warning,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Informational only \u2014 not a certified diagnosis.",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textFaint,
                )

                PrimaryAction(
                    text = "Read DTCs",
                    onClick = onRequestDtcs,
                    enabled = obdState is ObdConnectionState.Connected && dtcState !is ObdDtcState.Reading,
                )
            }
        }
    }
}

@Composable
private fun ObdTelemetryCard(
    obdState: ObdConnectionState,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val sample = (obdState as? ObdConnectionState.Connected)?.sample ?: ObdSample()
    val isConnected = obdState is ObdConnectionState.Connected

    WidgetContainer(
        title = "LIVE ECU TELEMETRY",
        icon = Icons.Outlined.Sensors,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AutoVerseSpacing.xs),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
            ) {
                // Top Row: Engine RPM and Vehicle Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
                ) {
                    TelemetryBlock(
                        title = "ENGINE RPM",
                        value = if (isConnected && sample.rpm != null) "${sample.rpm}" else "---",
                        unit = "RPM",
                        highlight = true,
                        modifier = Modifier.weight(1f),
                    )

                    TelemetryBlock(
                        title = "ECU SPEED",
                        value = if (isConnected && sample.speedKmh != null) "${sample.speedKmh}" else "---",
                        unit = "KM/H",
                        highlight = false,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Middle Row: Coolant Temp and Engine Load
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
                ) {
                    TelemetryBlock(
                        title = "COOLANT TEMP",
                        value = if (isConnected && sample.coolantC != null) "${sample.coolantC}" else "---",
                        unit = "°C",
                        highlight = false,
                        modifier = Modifier.weight(1f),
                    )

                    TelemetryBlock(
                        title = "ENGINE LOAD",
                        value = if (isConnected && sample.loadPct != null) "${sample.loadPct}" else "---",
                        unit = "%",
                        highlight = false,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Bottom Row: Throttle Position and Fuel Level
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
                ) {
                    TelemetryBlock(
                        title = "THROTTLE",
                        value = if (isConnected && sample.throttlePct != null) "${sample.throttlePct}" else "---",
                        unit = "%",
                        highlight = false,
                        modifier = Modifier.weight(1f),
                    )

                    TelemetryBlock(
                        title = "FUEL LEVEL",
                        value = if (isConnected && sample.fuelPct != null) "${sample.fuelPct}" else "---",
                        unit = "%",
                        highlight = false,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text(
                text = "Live SAE J1979 Mode 01 telemetry directly from vehicle ECU. Speed reported by ECU is distinct from GPS speed.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
            )
        }
    }
}

@Composable
private fun TelemetryBlock(
    title: String,
    value: String,
    unit: String,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current

    Box(
        modifier = modifier
            .clip(AutoVerseShapes.tile)
            .background(colors.surfaceHighlight)
            .padding(AutoVerseSpacing.md),
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = value,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) colors.accent else colors.textPrimary,
                    lineHeight = 34.sp,
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accent,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}
