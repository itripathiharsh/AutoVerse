package com.autoverse.launcher.core.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.autoverse.launcher.core.media.isNotificationAccessEnabled
import com.autoverse.launcher.core.media.notificationAccessSettingsIntent

/**
 * Everything the cockpit needs from the head unit, as one snapshot. The
 * first-run setup, the "finish setup" gate and the Home panels all read this,
 * so the same rules decide what counts as ready everywhere.
 */
data class CarSetupStatus(
    /** Precise location granted (the GPS speed and map need it, not just approximate). */
    val preciseLocation: Boolean,
    /** Any location grant, precise or approximate. */
    val anyLocation: Boolean,
    /** The unit's location service (GPS) switch is on. */
    val locationServiceOn: Boolean,
    /** Media-session access (notification-listener binding). */
    val mediaAccess: Boolean,
    /** READ_PHONE_STATE for the incoming-call panel. */
    val phoneState: Boolean,
    /** BLUETOOTH_CONNECT for an OBD-II adapter (always true below Android 12). */
    val bluetooth: Boolean,
    /** AutoVerse is the default Home app. */
    val defaultLauncher: Boolean,
    /** Reading music files (USB / SD / this unit) for the built-in player. */
    val musicFiles: Boolean = false,
) {
    /** The three things a driver cannot do without: position, GPS switched on, music. */
    val requiredDone: Int
        get() = listOf(preciseLocation, locationServiceOn, mediaAccess).count { it }

    val requiredTotal: Int get() = REQUIRED_COUNT

    val allRequiredGranted: Boolean get() = requiredDone == REQUIRED_COUNT

    companion object {
        const val REQUIRED_COUNT = 3

        /** Reads the live state from the platform. Cheap; call on every resume. */
        fun read(context: Context): CarSetupStatus {
            val fine = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = granted(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            return CarSetupStatus(
                preciseLocation = fine,
                anyLocation = fine || coarse,
                locationServiceOn = isLocationServiceOn(context),
                mediaAccess = isNotificationAccessEnabled(context),
                phoneState = granted(context, Manifest.permission.READ_PHONE_STATE),
                bluetooth = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    granted(context, Manifest.permission.BLUETOOTH_CONNECT),
                defaultLauncher = isDefaultLauncher(context),
                musicFiles = com.autoverse.launcher.core.music.hasAudioReadPermission(context),
            )
        }
    }
}

private fun granted(context: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** True when the device-wide location switch is on (independent of the app's permission). */
fun isLocationServiceOn(context: Context): Boolean {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return try {
        LocationManagerCompat.isLocationEnabled(manager)
    } catch (_: Exception) {
        false
    }
}

fun isDefaultLauncher(context: Context): Boolean = try {
    val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
    resolved?.activityInfo?.packageName == context.packageName
} catch (_: Exception) {
    false
}

/**
 * Opens a settings screen, trying each intent in turn; head-unit ROMs often
 * strip one of them. Returns false when none resolves so the UI can explain.
 */
private fun openFirst(context: Context, vararg intents: Intent): Boolean {
    for (intent in intents) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        } catch (_: Exception) {
            // try the next one
        }
    }
    return false
}

/** The system screen with the location (GPS) on/off switch. */
fun openLocationServiceSettings(context: Context): Boolean = openFirst(
    context,
    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
    Intent(Settings.ACTION_SETTINGS),
)

/** AutoVerse's own App info page, where a permanently denied permission can be allowed. */
fun openAppPermissionSettings(context: Context): Boolean = openFirst(
    context,
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    Intent(Settings.ACTION_SETTINGS),
)

/** The notification-access list where "AutoVerse" is switched on for music control. */
fun openMediaAccessSettings(context: Context): Boolean = openFirst(
    context,
    notificationAccessSettingsIntent(),
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
)

/** The Android "default home app" picker, falling back to a chooser. */
fun openDefaultLauncherSettings(context: Context): Boolean = openFirst(
    context,
    Intent(Settings.ACTION_HOME_SETTINGS),
    Intent.createChooser(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        "Set AutoVerse as your home app",
    ),
)

/**
 * What a location panel should offer the driver, or null when location is
 * fine. Pure so the wording is unit-tested.
 */
enum class LocationFix(val label: String, val explanation: String) {
    GRANT("Allow location", "AutoVerse needs location permission"),
    TURN_ON("Turn on location", "Location is switched off on this unit"),
}

fun locationFixFor(hasPermission: Boolean, serviceOn: Boolean): LocationFix? = when {
    !hasPermission -> LocationFix.GRANT
    !serviceOn -> LocationFix.TURN_ON
    else -> null
}
