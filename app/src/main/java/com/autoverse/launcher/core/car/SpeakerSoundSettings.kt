package com.autoverse.launcher.core.car

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.provider.Settings

/**
 * Opens the head unit's own sound settings (where the Fader and Balance
 * live), so the driver can move the fader to the rear while testing.
 */
object SpeakerSoundSettings {
    /**
     * Sound / EQ apps shipped on common aftermarket units (FYT, Topway,
     * Microntek, ...). Missing ones are simply skipped.
     */
    internal val KNOWN_PACKAGES = listOf(
        "com.syu.eq",
        "com.tw.eq",
        "com.microntek.eq",
        "com.ts.eq",
    )

    /** Tries the unit's EQ app, then any pre-installed sound app, then Android's sound settings. */
    fun open(context: Context): Boolean {
        val pm = context.packageManager
        val known = KNOWN_PACKAGES.mapNotNull { pkg -> runCatching { pm.getLaunchIntentForPackage(pkg) }.getOrNull() }
        val fallbacks = listOf(Intent(Settings.ACTION_SOUND_SETTINGS), Intent(Settings.ACTION_SETTINGS))
        for (intent in known + preinstalledSoundApps(context) + fallbacks) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (_: Exception) {
                // Not there or not exported: try the next one.
            }
        }
        return false
    }

    /** Launchable system apps whose package or activity name looks like an EQ / sound settings screen. */
    @Suppress("DEPRECATION") // queryIntentActivities(Intent, Int): the flags overload needs API 33
    private fun preinstalledSoundApps(context: Context): List<Intent> = try {
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        context.packageManager.queryIntentActivities(query, 0)
            .mapNotNull { it.activityInfo }
            .filter { info ->
                info.packageName != context.packageName &&
                    ((info.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    looksLikeSoundSettings(info.packageName, info.name)
            }
            .map { info ->
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setClassName(info.packageName, info.name)
            }
    } catch (_: Exception) {
        emptyList()
    }
}

private val SOUND_PACKAGE_SEGMENTS = setOf(
    "eq", "equalizer", "equaliser", "audioeffect", "audioeffects", "dsp",
    "soundsetting", "soundsettings", "audiosetting", "audiosettings", "fader",
)

private val SOUND_ACTIVITY_NAMES = setOf(
    "eq", "equalizer", "equaliser", "soundsetting", "soundsettings", "audiosetting",
    "audiosettings", "dsp", "fader", "balance", "soundeffect", "audioeffect",
)

/** Pure name check, unit-tested: "com.syu.eq" yes, "com.tw.music" no. */
internal fun looksLikeSoundSettings(packageName: String, activityName: String): Boolean {
    if (packageName.lowercase().split('.').any { it in SOUND_PACKAGE_SEGMENTS }) return true
    val simple = activityName.substringAfterLast('.').lowercase().removeSuffix("activity")
    return simple in SOUND_ACTIVITY_NAMES
}
