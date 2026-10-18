package com.autoverse.launcher.core.navigation

/**
 * Phase 18 — effective provider for the navigation page. The stored
 * preference always wins when it names a real app; when it is the system
 * default (no preference stored, or the preferred app is missing), Google
 * Maps is used if it is installed, because turn-by-turn is the page's
 * primary job. The stored preference itself is never modified, and
 * [ResolvedNavigationProvider.preferredMissing] is carried through so the
 * page can still say the preferred app is missing.
 */
fun navigationPageProvider(
    resolved: ResolvedNavigationProvider,
    providers: List<NavigationProvider>,
): ResolvedNavigationProvider {
    if (resolved.provider !is NavigationProvider.SystemDefault) return resolved
    val maps = providers.firstOrNull { it is NavigationProvider.GoogleMaps } ?: return resolved
    return ResolvedNavigationProvider(
        provider = maps,
        preferredMissing = resolved.preferredMissing,
    )
}
