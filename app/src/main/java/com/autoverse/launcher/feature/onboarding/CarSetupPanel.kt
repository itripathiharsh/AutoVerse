package com.autoverse.launcher.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.permissions.CarSetupStatus
import com.autoverse.launcher.core.permissions.openAppPermissionSettings
import com.autoverse.launcher.core.permissions.openDefaultLauncherSettings
import com.autoverse.launcher.core.permissions.openLocationServiceSettings
import com.autoverse.launcher.core.permissions.openMediaAccessSettings
import kotlinx.coroutines.delay

/**
 * The permission checklist a driver goes through once: every item says, in one
 * sentence, what it is for, and has one button that either asks Android or
 * opens the exact settings screen. Required items (location, GPS switched on,
 * music control) are marked; [onStatusChange] reports the live state so the
 * caller can keep "Continue" locked until they are done.
 *
 * Status is re-read on every resume and once a second while visible, because
 * the location switch can be flipped from the quick-settings shade without
 * AutoVerse ever pausing.
 */
@Composable
fun CarSetupPanel(
    onStatusChange: (CarSetupStatus) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Get your car ready",
    subtitle: String = "AutoVerse needs a few permissions to work like a real car " +
        "system. Each one is used only while AutoVerse is on screen.",
) {
    val context = LocalContext.current
    val colors = LocalAutoVerseColors.current
    var status by remember { mutableStateOf(CarSetupStatus.read(context)) }
    // A permission Android will no longer show a dialog for can only be
    // allowed from App info; we learn that after one denied request.
    var locationBlocked by remember { mutableStateOf(false) }
    var phoneBlocked by remember { mutableStateOf(false) }
    var bluetoothBlocked by remember { mutableStateOf(false) }

    fun refresh() {
        val next = CarSetupStatus.read(context)
        status = next
        onStatusChange(next)
    }

    LaunchedEffect(Unit) {
        onStatusChange(status)
        while (true) {
            delay(1_000)
            refresh()
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val fine = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        locationBlocked = !fine && !canAskAgain(context, Manifest.permission.ACCESS_FINE_LOCATION)
        refresh()
    }
    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        phoneBlocked = !ok && !canAskAgain(context, Manifest.permission.READ_PHONE_STATE)
        refresh()
    }
    val bluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { ok ->
        bluetoothBlocked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !ok &&
            !canAskAgain(context, Manifest.permission.BLUETOOTH_CONNECT)
        refresh()
    }

    val items = listOf(
        SetupItem(
            icon = Icons.Outlined.LocationOn,
            title = "Precise location",
            why = "Shows your car on the map, measures your speed from GPS and " +
                "finds local weather.",
            required = true,
            done = status.preciseLocation,
            actionLabel = when {
                locationBlocked -> "Open settings"
                status.anyLocation -> "Make precise"
                else -> "Allow"
            },
            onAction = {
                if (locationBlocked) {
                    openAppPermissionSettings(context)
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                }
            },
            hint = if (locationBlocked) "Android blocked the request. Tap Open settings → Permissions → Location → Allow." else null,
        ),
        SetupItem(
            icon = Icons.Outlined.GpsFixed,
            title = "GPS switched on",
            why = "Your head unit's location switch must be on, or the map and " +
                "speed can't find you.",
            required = true,
            done = status.locationServiceOn,
            actionLabel = "Turn on",
            onAction = { openLocationServiceSettings(context) },
        ),
        SetupItem(
            icon = Icons.Outlined.MusicNote,
            title = "Music control",
            why = "Shows the song from Spotify, YouTube Music or your USB player and " +
                "gives you play, pause and skip on the dashboard. Your notifications " +
                "are never read.",
            required = true,
            done = status.mediaAccess,
            actionLabel = "Allow",
            onAction = { openMediaAccessSettings(context) },
            hint = if (!status.mediaAccess) "In the list that opens, switch on AutoVerse." else null,
        ),
        SetupItem(
            icon = Icons.Outlined.Home,
            title = "Start with the car",
            why = "Makes AutoVerse the home screen, so it opens every time the car starts.",
            required = false,
            done = status.defaultLauncher,
            actionLabel = "Set as home",
            onAction = { openDefaultLauncherSettings(context) },
        ),
        SetupItem(
            icon = Icons.Outlined.Phone,
            title = "Incoming calls",
            why = "Shows who is calling on the dashboard so you don't look at your phone.",
            required = false,
            done = status.phoneState,
            actionLabel = if (phoneBlocked) "Open settings" else "Allow",
            onAction = {
                if (phoneBlocked) openAppPermissionSettings(context)
                else phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
            },
        ),
        SetupItem(
            icon = Icons.Outlined.Bluetooth,
            title = "OBD-II adapter",
            why = "Connects to a Bluetooth OBD-II plug to read RPM, engine temperature " +
                "and fault codes.",
            required = false,
            done = status.bluetooth,
            actionLabel = if (bluetoothBlocked) "Open settings" else "Allow",
            onAction = {
                if (bluetoothBlocked) {
                    openAppPermissionSettings(context)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            },
        ),
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.lg),
            ) {
                SetupHeader(
                    title = title,
                    subtitle = subtitle,
                    status = status,
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight(),
                )
                SetupList(
                    items = items,
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                SetupHeader(title = title, subtitle = subtitle, status = status)
                Spacer(Modifier.height(AutoVerseSpacing.md))
                SetupList(items = items, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Shown at launch after onboarding when a required item was revoked or
 * switched off later (e.g. GPS turned off in quick settings, permission
 * removed in App info). "Not now" hides it until AutoVerse restarts.
 */
@Composable
fun FinishSetupScreen(
    onContinue: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    var status by remember { mutableStateOf<CarSetupStatus?>(null) }
    val ready = status?.allRequiredGranted == true
    LaunchedEffect(ready) { if (ready) onContinue() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = AutoVerseSpacing.lg, vertical = AutoVerseSpacing.md),
    ) {
        CarSetupPanel(
            onStatusChange = { status = it },
            title = "Finish car setup",
            subtitle = "Something AutoVerse needs is off. Fix the REQUIRED items so the " +
                "map, speed and music work.",
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.height(AutoVerseSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.autoverse.launcher.core.design.components.TextAction(text = "Not now", onClick = onNotNow)
        }
    }
}

private data class SetupItem(
    val icon: ImageVector,
    val title: String,
    val why: String,
    val required: Boolean,
    val done: Boolean,
    val actionLabel: String,
    val onAction: () -> Unit,
    val hint: String? = null,
)

@Composable
private fun SetupHeader(
    title: String,
    subtitle: String,
    status: CarSetupStatus,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.Center) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = colors.accent, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(AutoVerseSpacing.md))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(AutoVerseSpacing.lg))
        val done = status.requiredDone
        val total = status.requiredTotal
        Text(
            text = if (status.allRequiredGranted) "All required items ready" else "$done of $total required items ready",
            style = MaterialTheme.typography.titleSmall,
            color = if (status.allRequiredGranted) colors.success else colors.textPrimary,
        )
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        LinearProgressIndicator(
            progress = { done / total.toFloat() },
            color = if (status.allRequiredGranted) colors.success else colors.accent,
            trackColor = colors.surfaceHighlight,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        Text(
            "No account, no ads, nothing tracked. Location never leaves the unit " +
                "except a rounded (~1 km) weather lookup.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textFaint,
        )
    }
}

@Composable
private fun SetupList(items: List<SetupItem>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
    ) {
        items.forEach { SetupCard(it) }
    }
}

@Composable
private fun SetupCard(item: SetupItem) {
    val colors = LocalAutoVerseColors.current
    val borderColor = when {
        item.done -> colors.success.copy(alpha = 0.6f)
        item.required -> colors.accent.copy(alpha = 0.7f)
        else -> colors.outline
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AutoVerseShapes.small)
            .background(colors.surface)
            .border(1.dp, borderColor, AutoVerseShapes.small)
            .padding(AutoVerseSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (item.done) colors.success.copy(alpha = 0.18f) else colors.surfaceHighlight),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = if (item.done) colors.success else colors.accent,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(AutoVerseSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(AutoVerseSpacing.xs))
                Tag(text = if (item.required) "REQUIRED" else "OPTIONAL", strong = item.required && !item.done)
            }
            Spacer(Modifier.height(2.dp))
            Text(item.why, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            if (item.hint != null && !item.done) {
                Spacer(Modifier.height(2.dp))
                Text(item.hint, style = MaterialTheme.typography.labelSmall, color = colors.warning)
            }
        }
        Spacer(Modifier.width(AutoVerseSpacing.md))
        if (item.done) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = colors.success, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ready", style = MaterialTheme.typography.labelLarge, color = colors.success)
            }
        } else {
            PrimaryAction(text = item.actionLabel, onClick = item.onAction)
        }
    }
}

@Composable
private fun Tag(text: String, strong: Boolean) {
    val colors = LocalAutoVerseColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (strong) colors.accent else colors.surfaceHighlight)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (strong) colors.onAccent else colors.textFaint,
        )
    }
}

private fun canAskAgain(context: Context, permission: String): Boolean {
    val activity = context.findActivity() ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
