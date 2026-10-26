package com.autoverse.launcher.core.navigation

/**
 * A navigation handoff target. Every provider keeps its own documented URI
 * format; AutoVerse never assumes two apps share one. [OpenOnly] covers
 * discovered navigation-capable apps with no public handoff format: they can
 * be opened, but never receive a destination.
 */
sealed interface NavigationProvider {

    /** Name shown in settings and on the navigation card. */
    val label: String

    /** Target app package; null means "let Android choose" (system default). */
    val packageName: String?

    /** Whether a documented destination handoff exists for this target. */
    val supportsDestination: Boolean

    /** Platform geo: scheme; the system resolves which app receives it. */
    data object SystemDefault : NavigationProvider {
        override val label = "System default"
        override val packageName: String? = null
        override val supportsDestination = true
    }

    /** Documented at https://developer.android.com/guide/components/google-maps-intents */
    data object GoogleMaps : NavigationProvider {
        override val label = "Google Maps"
        override val packageName = GOOGLE_MAPS_PACKAGE
        override val supportsDestination = true
    }

    /** Documented at https://developers.google.com/waze/deeplinks */
    data object Waze : NavigationProvider {
        override val label = "Waze"
        override val packageName = WAZE_PACKAGE
        override val supportsDestination = true
    }

    /** A discovered navigation-capable app with no documented handoff format. */
    data class OpenOnly(
        override val packageName: String,
        override val label: String,
    ) : NavigationProvider {
        override val supportsDestination = false
    }
}

/** Whether the target has an app to open at all. */
val NavigationProvider.supportsOpenApp: Boolean
    get() = packageName != null

/**
 * Google Maps turn-by-turn handoff URI (official intents documentation):
 * `google.navigation:q=<lat,lng>` or `google.navigation:q=<query>`, launched
 * as ACTION_VIEW with the Maps package pinned.
 */
fun googleMapsDestinationUri(destination: NavigationDestination): String = when (destination) {
    is NavigationDestination.Coordinates -> "google.navigation:q=" +
        "${formatCoordinate(destination.latitude)},${formatCoordinate(destination.longitude)}"
    is NavigationDestination.Query ->
        "google.navigation:q=${encodeUriComponent(destination.text)}"
}

/**
 * Waze handoff URI (official deep-link documentation): the `waze://` scheme
 * replaces the `https://waze.com/ul` base when the app is guaranteed
 * installed, with the same parameters and `navigate=yes`.
 */
fun wazeDestinationUri(destination: NavigationDestination): String = when (destination) {
    is NavigationDestination.Coordinates -> "waze://?ll=" +
        "${formatCoordinate(destination.latitude)},${formatCoordinate(destination.longitude)}" +
        "&navigate=yes"
    is NavigationDestination.Query ->
        "waze://?q=${encodeUriComponent(destination.text)}&navigate=yes"
}

/**
 * Platform geo: handoff. Coordinates keep both the bare position and a
 * matching q= marker; queries use the documented `geo:0,0?q=` search form.
 */
fun geoDestinationUri(destination: NavigationDestination): String = when (destination) {
    is NavigationDestination.Coordinates -> {
        val position = "${formatCoordinate(destination.latitude)}," +
            formatCoordinate(destination.longitude)
        "geo:$position?q=$position"
    }
    is NavigationDestination.Query ->
        "geo:0,0?q=${encodeUriComponent(destination.text)}"
}

/** Destination URI for [provider], or null when it has no documented format. */
fun navigationDestinationUri(
    provider: NavigationProvider,
    destination: NavigationDestination,
): String? = when (provider) {
    NavigationProvider.SystemDefault -> geoDestinationUri(destination)
    NavigationProvider.GoogleMaps -> googleMapsDestinationUri(destination)
    NavigationProvider.Waze -> wazeDestinationUri(destination)
    is NavigationProvider.OpenOnly -> null
}

/** The provider a handoff will use, plus stale-preference information. */
data class ResolvedNavigationProvider(
    val provider: NavigationProvider,
    val preferredMissing: Boolean,
)

/**
 * Display state for [preferredPackage]. Before discovery has run ([providers]
 * empty) the preference is shown as-is — the launch path re-checks the real
 * install state at tap time, so nothing is downgraded on a guess. A stored
 * package missing from a completed discovery is flagged
 * [ResolvedNavigationProvider.preferredMissing] (the provider stays the
 * preferred one, so the label and the launch always name the same app).
 */
fun resolveNavigationProvider(
    preferredPackage: String?,
    providers: List<NavigationProvider>,
): ResolvedNavigationProvider {
    val preferred = providerForPackage(preferredPackage)
    if (preferredPackage == null || providers.isEmpty()) {
        return ResolvedNavigationProvider(provider = preferred, preferredMissing = false)
    }
    val match = providers.firstOrNull { it.packageName == preferredPackage }
    return ResolvedNavigationProvider(
        provider = match ?: preferred,
        preferredMissing = match == null,
    )
}
