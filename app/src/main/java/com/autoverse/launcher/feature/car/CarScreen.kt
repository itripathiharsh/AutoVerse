package com.autoverse.launcher.feature.car

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.core.car.CarController
import com.autoverse.launcher.core.car.HealthReport
import com.autoverse.launcher.core.car.Reminder
import com.autoverse.launcher.core.car.ReminderKind
import com.autoverse.launcher.core.car.ReminderUrgency
import com.autoverse.launcher.core.car.reminderDetail
import com.autoverse.launcher.core.car.reminderUrgency
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AppNavigationRail
import com.autoverse.launcher.core.design.components.AutoVerseTextField
import com.autoverse.launcher.core.design.components.CockpitCard
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.RailDestination
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.location.SpeedometerData
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class CarSection(val title: String, val icon: ImageVector) {
    TRIPS("Trips & fuel", Icons.Outlined.Route),
    REMINDERS("Reminders", Icons.Outlined.Notifications),
    SPEAKERS("Speaker test", Icons.Outlined.Speaker),
    HEALTH("Head unit check", Icons.Outlined.DeveloperBoard),
    PHONE("Phone", Icons.Outlined.PhoneAndroid),
}

/** The Car page: everything about the car and the unit, in one place. */
@Composable
fun CarScreen(
    car: CarController,
    speedometerData: SpeedometerData,
    onOpenSpeedometer: () -> Unit,
    onOpenObd: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenAppDrawer: () -> Unit,
    onOpenNavigation: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    var section by rememberSaveable { mutableStateOf(CarSection.TRIPS) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = AutoVerseSpacing.screenHorizontal, vertical = AutoVerseSpacing.screenVertical),
    ) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.gridGap)) {
            AppNavigationRail(
                selected = RailDestination.CAR,
                onOpenHome = onOpenHome,
                onOpenAppDrawer = onOpenAppDrawer,
                onOpenNavigation = onOpenNavigation,
                onOpenSettings = onOpenSettings,
                onOpenSpeedometer = onOpenSpeedometer,
                onOpenObd = onOpenObd,
                onOpenDiagnostics = onOpenDiagnostics,
                modifier = Modifier.width(AutoVerseSpacing.railWidth).fillMaxHeight(),
            )
            CockpitCard(modifier = Modifier.width(250.dp).fillMaxHeight()) {
                Text("Car", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
                Spacer(Modifier.height(AutoVerseSpacing.sm))
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xxs)) {
                    CarSection.entries.forEach { s ->
                        NavItem(s.icon, s.title, selected = section == s) { section = s }
                    }
                    Spacer(Modifier.height(AutoVerseSpacing.sm))
                    NavItem(Icons.Outlined.Speed, "Speedometer", selected = false, onClick = onOpenSpeedometer)
                    NavItem(Icons.Outlined.Sensors, "OBD-II engine data", selected = false, onClick = onOpenObd)
                }
            }
            CockpitCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (section) {
                    CarSection.TRIPS -> TripsSection(car)
                    CarSection.REMINDERS -> RemindersSection(car)
                    CarSection.SPEAKERS -> SpeakerSection(car)
                    CarSection.HEALTH -> HealthSection(car, onOpenDiagnostics)
                    CarSection.PHONE -> PhoneSection()
                }
            }
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.tile)
            .background(if (selected) colors.surfaceHighlight else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .heightIn(min = AutoVerseSpacing.touchComfort)
            .padding(horizontal = AutoVerseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) colors.accent else colors.textSecondary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(AutoVerseSpacing.sm))
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.textPrimary else colors.textSecondary)
    }
}

// ---- trips & fuel ---------------------------------------------------------------

@Composable
private fun TripsSection(car: CarController) {
    val colors = LocalAutoVerseColors.current
    val trips by car.trips.collectAsStateWithLifecycle()
    val fuel by car.fuel.collectAsStateWithLifecycle()
    var price by remember(fuel) { mutableStateOf(trimNum(fuel.pricePerLitre)) }
    var mileage by remember(fuel) { mutableStateOf(trimNum(fuel.kmPerLitre)) }
    val today = LocalDate.now()
    val zone = ZoneId.systemDefault()
    val todayKm = trips.filter { Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate() == today }.sumOf { it.distanceKm } + car.tripInProgressKm
    val monthKm = trips.filter { Instant.ofEpochMilli(it.startMs).atZone(zone).toLocalDate().withDayOfMonth(1) == today.withDayOfMonth(1) }.sumOf { it.distanceKm } + car.tripInProgressKm

    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
            StatTile("Today", "%.1f km".format(todayKm), "₹" + fuel.costFor(todayKm).toInt(), Modifier.weight(1f))
            StatTile("This month", "%.0f km".format(monthKm), "₹" + fuel.costFor(monthKm).toInt(), Modifier.weight(1f))
            StatTile("Fuel this month", "%.1f L".format(fuel.litresFor(monthKm)), "at ${trimNum(fuel.kmPerLitre)} km/L", Modifier.weight(1f))
        }
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
            Icon(Icons.Outlined.LocalGasStation, null, tint = colors.accent, modifier = Modifier.padding(bottom = 16.dp))
            Column(Modifier.weight(1f)) {
                Text("Fuel price (₹ per litre)", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                AutoVerseTextField(value = price, onValueChange = { price = it }, placeholder = "105", modifier = Modifier.fillMaxWidth())
            }
            Column(Modifier.weight(1f)) {
                Text("Your car's mileage (km per litre)", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                AutoVerseTextField(value = mileage, onValueChange = { mileage = it }, placeholder = "15", modifier = Modifier.fillMaxWidth())
            }
            PrimaryAction(text = "Save", onClick = {
                val p = price.toDoubleOrNull(); val m = mileage.toDoubleOrNull()
                if (p != null && m != null) car.setFuel(p, m)
            })
        }
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent trips", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
            if (trips.isNotEmpty()) TextAction(text = "Clear", onClick = car::clearTrips)
        }
        if (trips.isEmpty()) {
            Text(
                "Trips are recorded automatically whenever you drive with AutoVerse open — distance, time, top speed and fuel cost.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
        val fmt = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.ENGLISH)
        LazyColumn {
            items(trips, key = { it.startMs }) { t ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(Instant.ofEpochMilli(t.startMs).atZone(zone).format(fmt), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("${t.durationMin} min · avg ${t.avgSpeedKmh} km/h · top ${t.maxSpeedKmh} km/h", style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("%.1f km".format(t.distanceKm), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                        Text("₹" + fuel.costFor(t.distanceKm).toInt(), style = MaterialTheme.typography.bodySmall, color = colors.success)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, sub: String, modifier: Modifier) {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier
            .clip(AutoVerseShapes.small)
            .background(colors.surfaceHighlight)
            .padding(AutoVerseSpacing.md),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1)
        Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.success, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---- reminders -------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersSection(car: CarController) {
    val colors = LocalAutoVerseColors.current
    val reminders by car.reminders.collectAsStateWithLifecycle()
    val odometer by car.odometerKm.collectAsStateWithLifecycle()
    var odoText by remember(odometer) { mutableStateOf(odometer?.toString().orEmpty()) }
    var serviceKm by remember(reminders) { mutableStateOf(reminders.firstOrNull { it.kind == ReminderKind.SERVICE }?.dueOdometerKm?.toString().orEmpty()) }
    var picking by remember { mutableStateOf<ReminderKind?>(null) }
    val today = LocalDate.now()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Never miss a renewal", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        Text("Home shows a banner 15 days before anything is due.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(AutoVerseSpacing.md))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs)) {
            Text("Odometer now", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary, modifier = Modifier.width(150.dp))
            AutoVerseTextField(value = odoText, onValueChange = { odoText = it.filter(Char::isDigit) }, placeholder = "km on the dashboard", modifier = Modifier.weight(1f))
            PrimaryAction(text = "Save", onClick = { odoText.toIntOrNull()?.let(car::setOdometer) })
        }
        Text(
            if (odometer != null) "AutoVerse keeps it up to date as you drive: now $odometer km." else "Enter it once; AutoVerse adds the kilometres you drive.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textFaint,
        )
        Spacer(Modifier.height(AutoVerseSpacing.md))
        ReminderKind.entries.forEach { kind ->
            val r = reminders.firstOrNull { it.kind == kind }
            val urgency = r?.let { reminderUrgency(it, today, odometer) } ?: ReminderUrgency.OK
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(AutoVerseShapes.small)
                    .background(colors.surfaceHighlight)
                    .padding(AutoVerseSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(12.dp).clip(CircleShape).background(
                        when (urgency) {
                            ReminderUrgency.DUE -> colors.danger
                            ReminderUrgency.SOON -> colors.warning
                            ReminderUrgency.OK -> if (r == null) colors.outline else colors.success
                        },
                    ),
                )
                Spacer(Modifier.width(AutoVerseSpacing.sm))
                Column(Modifier.weight(1f)) {
                    Text(kind.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    Text(
                        r?.let { reminderDetail(it, today, odometer) + (it.dueDate?.let { d -> "  (" + d.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) + ")" } ?: "") } ?: "Not set",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                if (kind == ReminderKind.SERVICE) {
                    AutoVerseTextField(value = serviceKm, onValueChange = { serviceKm = it.filter(Char::isDigit) }, placeholder = "at km", modifier = Modifier.width(130.dp))
                    TextAction(text = "Set km", onClick = {
                        car.setReminder(Reminder(kind, r?.dueDate, serviceKm.toIntOrNull()))
                    })
                }
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = "Pick date",
                    tint = colors.accent,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable { picking = kind }.padding(8.dp),
                )
                if (r != null) TextAction(text = "Clear", onClick = { car.clearReminder(kind) })
            }
            Spacer(Modifier.height(AutoVerseSpacing.xs))
        }
    }

    picking?.let { kind ->
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextAction(text = "Set", onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        val old = reminders.firstOrNull { it.kind == kind }
                        car.setReminder(Reminder(kind, date, old?.dueOdometerKm))
                    }
                    picking = null
                })
            },
            dismissButton = { TextAction(text = "Cancel", onClick = { picking = null }) },
        ) { DatePicker(state = state) }
    }
}

// ---- speaker test ----------------------------------------------------------------

/** Per-speaker test for 2 to 6 speakers + subwoofer; the panel lives in SpeakerTestPanel.kt. */
@Composable
private fun SpeakerSection(car: CarController) {
    SpeakerTestPanel(car)
}

// ---- head unit check -------------------------------------------------------------

@Composable
private fun HealthSection(car: CarController, onOpenDiagnostics: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    val context = LocalContext.current
    val report by car.healthReport.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { car.refreshHealth() }
    val r: HealthReport = report ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(r.unitClass.label, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    Text("${r.model} · ${r.androidVersion} · ${r.cpuCores} cores" + (r.cpuMaxMhz?.let { " up to ${it} MHz" } ?: ""), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
                TextAction(text = "Re-check", onClick = car::refreshHealth)
            }
            Text(r.unitClass.advice, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
                Meter("Memory in use", r.ramUsedPercent / 100f, "${r.totalRamMb - r.availRamMb} of ${r.totalRamMb} MB", Modifier.weight(1f))
                val usedStorage = if (r.storageTotalGb > 0) (1 - r.storageFreeGb / r.storageTotalGb).toFloat() else 0f
                Meter("Storage used", usedStorage, "%.1f GB free of %.0f GB".format(r.storageFreeGb, r.storageTotalGb), Modifier.weight(1f))
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(AutoVerseShapes.small).background(colors.surfaceHighlight).padding(AutoVerseSpacing.md)) {
                Text("What to do", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                r.tips.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary) }
            }
        }
        if (!r.usageAccess) {
            item {
                Row(
                    Modifier.clip(AutoVerseShapes.small).background(colors.surfaceHighlight).padding(AutoVerseSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "See which apps keep the unit busy and which you never use: allow Usage access for AutoVerse (it only counts how long apps are open).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryAction(text = "Allow", onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    })
                }
            }
        } else {
            if (r.busiestApps.isNotEmpty()) {
                item { Text("Busiest apps (last 24 h)", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary) }
                items(r.busiestApps, key = { "b" + it.packageName }) { a ->
                    AppRow(a.label, "${a.foregroundMinutes24h} min on screen", "Manage") { openAppInfo(context, a.packageName) }
                }
            }
            if (r.unusedApps.isNotEmpty()) {
                item { Text("Not opened in 30+ days — disable or uninstall to free memory", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary) }
                items(r.unusedApps, key = { "u" + it.packageName }) { a ->
                    AppRow(
                        a.label,
                        (if (a.preinstalled) "Pre-installed · " else "") + (a.lastUsedDaysAgo?.let { "last opened $it days ago" } ?: "not opened in the last 2 months"),
                        if (a.preinstalled) "Disable" else "Uninstall",
                    ) { openAppInfo(context, a.packageName) }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(AutoVerseShapes.small).background(colors.surfaceHighlight).padding(AutoVerseSpacing.md)) {
                Text("AutoVerse storage", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Text("Road data for navigation: ${r.autoverseRoadDataMb} MB · Map pictures: ${r.autoverseMapCacheMb} MB (trimmed automatically)", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                Row {
                    if (r.autoverseRoadDataMb > 0) TextAction(text = "Delete road data", onClick = car::clearRoadData)
                    TextAction(text = "Detailed diagnostics", onClick = onOpenDiagnostics)
                }
            }
        }
    }
}

@Composable
private fun Meter(title: String, fraction: Float, detail: String, modifier: Modifier) {
    val colors = LocalAutoVerseColors.current
    Column(modifier.clip(AutoVerseShapes.small).background(colors.surfaceHighlight).padding(AutoVerseSpacing.md)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Text("${(fraction * 100).toInt()}%", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        LinearProgressIndicator(
            progress = { fraction.coerceIn(0f, 1f) },
            color = if (fraction > 0.85f) colors.danger else if (fraction > 0.7f) colors.warning else colors.success,
            trackColor = colors.outline,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
        )
        Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
    }
}

@Composable
private fun AppRow(title: String, detail: String, action: String, onAction: () -> Unit) {
    val colors = LocalAutoVerseColors.current
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textFaint)
        }
        TextAction(text = action, onClick = onAction)
    }
}

private fun openAppInfo(context: android.content.Context, pkg: String) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private fun trimNum(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(Locale.US, v)
