package com.autoverse.launcher.core.navigation

/**
 * Navigation handoff surface. Implementations own provider discovery and
 * documented intent launching; callers only see providers, destinations and
 * outcomes. Expected failures are returned as [NavigationOutcome] values,
 * never thrown.
 */
interface NavigationRepository {

    /** Navigation targets available on this device; system default first. */
    suspend fun availableProviders(): List<NavigationProvider>

    /**
     * Launches [provider] with [destination], or opens the app itself when
     * [destination] is null.
     */
    suspend fun launch(
        destination: NavigationDestination?,
        provider: NavigationProvider,
    ): NavigationOutcome
}
