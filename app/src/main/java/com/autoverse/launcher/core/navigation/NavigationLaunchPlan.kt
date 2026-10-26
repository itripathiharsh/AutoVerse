package com.autoverse.launcher.core.navigation

import android.content.Intent

const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
const val WAZE_PACKAGE = "com.waze"

/** Flags for every handoff: AutoVerse is a launcher, so targets get their own task. */
const val HANDOFF_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED

/**
 * Maps a stored package preference to its provider without needing the
 * discovered-provider list. Launches resolve the provider this way at tap
 * time, so a slow app scan can never downgrade a Google Maps tap to the
 * package-less system default.
 */
fun providerForPackage(packageName: String?, label: String? = null): NavigationProvider =
    when (packageName) {
        null -> NavigationProvider.SystemDefault
        GOOGLE_MAPS_PACKAGE -> NavigationProvider.GoogleMaps
        WAZE_PACKAGE -> NavigationProvider.Waze
        else -> NavigationProvider.OpenOnly(packageName, label ?: packageName)
    }

/** Google Maps install state as AutoVerse last read it (UNKNOWN until the first scan). */
enum class MapsStatus { UNKNOWN, INSTALLED, NOT_INSTALLED }

/** Display state from a provider discovery; an empty list means discovery hasn't run. */
fun mapsStatusOf(providers: List<NavigationProvider>): MapsStatus = when {
    providers.isEmpty() -> MapsStatus.UNKNOWN
    providers.any { it is NavigationProvider.GoogleMaps } -> MapsStatus.INSTALLED
    else -> MapsStatus.NOT_INSTALLED
}

/** Install state of a pinned target package, read from PackageManager at tap time. */
enum class PackageState { MISSING, DISABLED, ENABLED }

/**
 * One concrete ACTION_VIEW attempt. [packageName] null means Android picks
 * the handler (system default geo: chooser).
 */
data class ViewIntentSpec(
    val uri: String,
    val packageName: String?,
    val flags: Int = HANDOFF_FLAGS,
)

/**
 * The launch decision for one tap, computed before any intent is fired.
 * [Rejected] carries the precise reason; [OpenApp] means "use the package's
 * own launcher intent"; [View] lists ACTION_VIEW attempts in priority order —
 * the first one that resolves to an activity is started.
 */
sealed interface NavigationLaunchPlan {
    data class Rejected(val outcome: NavigationOutcome) : NavigationLaunchPlan
    data class OpenApp(val packageName: String, val flags: Int = HANDOFF_FLAGS) : NavigationLaunchPlan
    data class View(val attempts: List<ViewIntentSpec>) : NavigationLaunchPlan
}

/**
 * Pure planner for a navigation tap. Pinned providers are checked against
 * their real install state first, so a missing or disabled app yields its own
 * message instead of a generic failure. Google Maps destinations try the
 * documented turn-by-turn scheme first and fall back to Maps' geo: search,
 * both pinned to the Maps package.
 */
fun planNavigationLaunch(
    provider: NavigationProvider,
    destination: NavigationDestination?,
    packageState: PackageState,
): NavigationLaunchPlan {
    val pinned = provider.packageName
    if (pinned != null) {
        when (packageState) {
            PackageState.MISSING -> return NavigationLaunchPlan.Rejected(NavigationOutcome.NOT_INSTALLED)
            PackageState.DISABLED -> return NavigationLaunchPlan.Rejected(NavigationOutcome.APP_DISABLED)
            PackageState.ENABLED -> Unit
        }
    }
    if (destination == null) {
        return if (pinned != null) {
            NavigationLaunchPlan.OpenApp(pinned)
        } else {
            // "Open the map app" with no app chosen: the empty geo: query opens
            // whichever map app the user picks, without inventing a place.
            NavigationLaunchPlan.View(listOf(ViewIntentSpec(uri = "geo:0,0", packageName = null)))
        }
    }
    if (destination is NavigationDestination.Query && destination.text.isBlank()) {
        return NavigationLaunchPlan.Rejected(NavigationOutcome.INVALID_DESTINATION)
    }
    if (!provider.supportsDestination) {
        return NavigationLaunchPlan.Rejected(NavigationOutcome.NOT_SUPPORTED)
    }
    val attempts = when (provider) {
        NavigationProvider.GoogleMaps -> listOf(
            ViewIntentSpec(googleMapsDestinationUri(destination), GOOGLE_MAPS_PACKAGE),
            ViewIntentSpec(geoDestinationUri(destination), GOOGLE_MAPS_PACKAGE),
        )
        NavigationProvider.Waze -> listOf(ViewIntentSpec(wazeDestinationUri(destination), WAZE_PACKAGE))
        NavigationProvider.SystemDefault -> listOf(ViewIntentSpec(geoDestinationUri(destination), null))
        is NavigationProvider.OpenOnly -> emptyList()
    }
    return NavigationLaunchPlan.View(attempts)
}
