package com.autoverse.launcher.feature.probe

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.autoverse.launcher.core.design.components.AutoVerseScreen
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.SectionLabel
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.map.ApproachAssessment
import com.autoverse.launcher.core.map.EmbedApproach
import com.autoverse.launcher.core.map.MapEmbedFacts
import com.autoverse.launcher.core.map.assessEmbeddingApproaches
import com.autoverse.launcher.core.map.overallRecommendation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
private const val MAX_LOG_EVENTS = 12

/**
 * 0.28.1 — minimal proof-of-concept probe for displaying the installed Google
 * Maps beside AutoVerse WITHOUT an API key. Read-only diagnostics plus three
 * launch probes (adjacent split flag, freeform bounds, same-task). It changes
 * nothing about the shipped screens; a full rebuild follows only if a probe
 * proves the platform supports the layout on this exact firmware.
 */
@Composable
fun MapEmbedProbeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val facts by produceState<MapEmbedFacts?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { collectFacts(context) }
    }
    val assessments = facts?.let { assessEmbeddingApproaches(it) }.orEmpty()
    val inMultiWindow = remember { mutableStateOf(false) }
    val log = remember { mutableStateListOf<String>() }

    fun appendLog(line: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        log.add(0, "$stamp  $line")
        while (log.size > MAX_LOG_EVENTS) log.removeAt(log.size - 1)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val multiWindow = (context as? android.app.Activity)?.isInMultiWindowModeCompat() == true
                inMultiWindow.value = multiWindow
                appendLog("Returned to probe — inMultiWindowMode=$multiWindow")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AutoVerseScreen(
        title = "Map embedding probe",
        onBack = onBack,
        modifier = modifier,
    ) {
        Text(
            text = "Diagnostic only. Collects this unit's real windowing capabilities " +
                "and live-tests each no-key approach for keeping Google Maps visible " +
                "beside AutoVerse. Nothing here changes the app.",
            style = MaterialTheme.typography.bodyMedium,
        )

        facts?.let { collected ->
            WidgetContainer(title = "DEVICE", icon = Icons.Outlined.Map) {
                KeyValue(label = "Android", value = "${collected.versionRelease} (SDK ${collected.sdkInt})")
                KeyValue(label = "Manufacturer", value = collected.manufacturer)
                KeyValue(label = "Model", value = collected.model)
                KeyValue(label = "Build", value = collected.fingerprint)
                KeyValue(label = "Freeform windows", value = yesNo(collected.freeformSupported))
                KeyValue(
                    label = "Activities on secondary display",
                    value = yesNo(collected.activitiesOnSecondaryDisplaySupported),
                )
                KeyValue(
                    label = "Dev option: force resizable activities",
                    value = collected.forceResizableActivities?.let { yesNo(it) } ?: "unreadable",
                )
            }

            WidgetContainer(title = "GOOGLE MAPS", icon = Icons.Outlined.Map) {
                KeyValue(label = "Installed", value = yesNo(collected.mapsInstalled))
                collected.mapsVersionName?.let { version ->
                    KeyValue(label = "Version", value = version)
                }
                KeyValue(
                    label = "Resizeable for multi-window",
                    value = collected.mapsResizeable?.let { yesNo(it) } ?: "not reported",
                )
            }

            WidgetContainer(title = "VERDICTS", icon = Icons.Outlined.Map) {
                assessments.forEach { assessment ->
                    KeyValue(
                        label = assessment.approach.displayName,
                        value = verdictLabel(assessment.viable),
                    )
                    Text(
                        text = assessment.explanation,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(AutoVerseSpacing.xs))
                }
                Text(
                    text = overallRecommendation(collected, assessments),
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            ProbeActions(
                facts = collected,
                inMultiWindow = inMultiWindow.value,
                onLog = ::appendLog,
            )
        } ?: Text(text = "Reading device capabilities…")

        if (log.isNotEmpty()) {
            WidgetContainer(title = "PROBE LOG (newest first)", icon = Icons.Outlined.Map) {
                log.forEach { event ->
                    Text(
                        text = event,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.size(AutoVerseSpacing.xs))
                TextAction(text = "Clear log", onClick = { log.clear() })
            }
        }
    }
}

@Composable
private fun ProbeActions(
    facts: MapEmbedFacts,
    inMultiWindow: Boolean,
    onLog: (String) -> Unit,
) {
    val context = LocalContext.current
    WidgetContainer(title = "LIVE PROBES", icon = Icons.Outlined.Map) {
        Text(
            text = "In split-screen right now: ${yesNo(inMultiWindow)}. Enter split-screen " +
                "from the system UI first (recents → split), then run probe 1 for the " +
                "adjacent placement.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.size(AutoVerseSpacing.sm))
        PrimaryAction(
            text = "1 · Launch Maps with adjacent flag",
            onClick = {
                onLog("Probe 1 sent: ACTION_VIEW geo + NEW_TASK|LAUNCH_ADJACENT")
                launchMaps(context, onLog, options = null) { intent ->
                    intent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            PrimaryAction(
                text = "2 · Freeform bounds (left 60%)",
                enabled = facts.freeformSupported,
                onClick = {
                    onLog("Probe 2 sent: launch bounds left 60% of display")
                    val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        ActivityOptions.makeBasic().setLaunchBounds(
                            displayBounds(context, fraction = 0.6f),
                        )
                    } else {
                        null
                    }
                    launchMaps(
                        context,
                        onLog,
                        options = options?.toBundle(),
                    ) { intent ->
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT,
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                text = "3 · Into this task",
                onClick = {
                    onLog("Probe 3 sent: no NEW_TASK — asks Maps into the probe's task")
                    launchMapsIntoTask(context, onLog)
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        Text(
            text = "After each probe, press back/return here: the log records whether " +
                "the system entered multi-window and whether AutoVerse stayed visible " +
                "beside Maps' real UI. That outcome decides the implementation.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Runs [configure] on a geo intent, starts it, and logs the outcome. */
private fun launchMaps(
    context: Context,
    onLog: (String) -> Unit,
    options: Bundle?,
    configure: (Intent) -> Unit,
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        .setPackage(GOOGLE_MAPS_PACKAGE)
    configure(intent)
    try {
        context.startActivity(intent, options)
    } catch (failure: Exception) {
        onLog("Maps launch failed: ${failure.javaClass.simpleName}")
    }
}

private fun launchMapsIntoTask(context: Context, onLog: (String) -> Unit) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        .setPackage(GOOGLE_MAPS_PACKAGE)
    try {
        context.startActivity(intent)
        onLog("Maps opened — if it covered the whole screen, it started its own " +
            "task (expected for singleTask apps) and cannot embed here.")
    } catch (failure: Exception) {
        onLog("Maps launch failed: ${failure.javaClass.simpleName}")
    }
}

/** Left [fraction] of the real display, in pixels, for launch bounds. */
private fun displayBounds(context: Context, fraction: Float): android.graphics.Rect {
    val metrics = context.resources.displayMetrics
    return android.graphics.Rect(
        0,
        0,
        (metrics.widthPixels * fraction).toInt(),
        metrics.heightPixels,
    )
}

private fun android.app.Activity.isInMultiWindowModeCompat(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInMultiWindowMode else false

/** Collects every fact with public APIs only; safe to read off the main thread. */
internal fun collectFacts(context: Context): MapEmbedFacts {
    val packageManager = context.packageManager
    val has = { feature: String ->
        try {
            packageManager.hasSystemFeature(feature)
        } catch (_: Exception) {
            false
        }
    }
    val mapsInfo = try {
        packageManager.getApplicationInfo(GOOGLE_MAPS_PACKAGE, 0)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
    val mapsVersion = try {
        packageManager.getPackageInfo(GOOGLE_MAPS_PACKAGE, 0).versionName
    } catch (_: Exception) {
        null
    }
    return MapEmbedFacts(
        sdkInt = Build.VERSION.SDK_INT,
        versionRelease = Build.VERSION.RELEASE,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        fingerprint = Build.FINGERPRINT,
        freeformSupported = has(PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT),
        activitiesOnSecondaryDisplaySupported = has(
            PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAYS,
        ),
        mapsInstalled = mapsInfo != null,
        mapsVersionName = mapsVersion,
        mapsResizeable = mapsInfo?.let { it.flags and resizeableFlag() != 0 },
        forceResizableActivities = readForceResizableActivities(context),
    )
}

private fun resizeableFlag(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        android.content.pm.ApplicationInfo.FLAG_RESIZEABLE_FOR_SCREENS
    } else {
        0
    }

/** Read-only peek at the "Force activities to be resizable" developer option. */
private fun readForceResizableActivities(context: Context): Boolean? = try {
    Settings.Global.getInt(context.contentResolver, "force_resizable_activities", 0) != 0
} catch (_: Exception) {
    null
}

@Composable
private fun KeyValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.size(AutoVerseSpacing.sm))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun yesNo(value: Boolean): String = if (value) "yes" else "no"

private fun verdictLabel(viable: Boolean?): String = when (viable) {
    true -> "supported by platform rules"
    false -> "not possible on this device"
    null -> "needs the live probe"
}
