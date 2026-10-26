package com.autoverse.launcher.core.navigation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [NavigationRepository] over PackageManager and documented launch intents.
 * Every tap is planned by [planNavigationLaunch] against the package's real
 * install state at that moment; every ACTION_VIEW attempt is resolved before
 * it is started, so "nothing can handle this" is reported instead of a
 * silent no-op.
 */
class SystemNavigationRepository(context: Context) : NavigationRepository {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val selfPackage = appContext.packageName

    override suspend fun availableProviders(): List<NavigationProvider> =
        withContext(Dispatchers.IO) {
            val providers = mutableListOf<NavigationProvider>(NavigationProvider.SystemDefault)
            if (packageState(GOOGLE_MAPS_PACKAGE) == PackageState.ENABLED) {
                providers += NavigationProvider.GoogleMaps
            }
            if (packageState(WAZE_PACKAGE) == PackageState.ENABLED) {
                providers += NavigationProvider.Waze
            }
            providers += discoveredOpenOnlyProviders()
            providers
        }

    override suspend fun launch(
        destination: NavigationDestination?,
        provider: NavigationProvider,
    ): NavigationOutcome = withContext(Dispatchers.IO) {
        val state = provider.packageName?.let(::packageState) ?: PackageState.ENABLED
        when (val plan = planNavigationLaunch(provider, destination, state)) {
            is NavigationLaunchPlan.Rejected -> plan.outcome
            is NavigationLaunchPlan.OpenApp -> openApp(plan)
            is NavigationLaunchPlan.View -> openView(plan, providerIsPinned = provider.packageName != null)
        }
    }

    /**
     * MISSING covers both "not installed" and "not visible to AutoVerse";
     * the manifest's <queries> block declares Google Maps and Waze, so for
     * those two packages MISSING really means not installed.
     */
    private fun packageState(packageName: String): PackageState = try {
        val info = packageManager.getApplicationInfo(packageName, 0)
        if (info.enabled) PackageState.ENABLED else PackageState.DISABLED
    } catch (_: PackageManager.NameNotFoundException) {
        PackageState.MISSING
    }

    private fun discoveredOpenOnlyProviders(): List<NavigationProvider> {
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        return try {
            packageManager.queryIntentActivities(geoIntent, 0)
                .map { it.activityInfo.packageName }
                .distinct()
                .filter { it != selfPackage && it != GOOGLE_MAPS_PACKAGE && it != WAZE_PACKAGE }
                .map { NavigationProvider.OpenOnly(it, appLabelOf(it)) }
                .sortedBy { it.label.lowercase() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun appLabelOf(packageName: String): String = try {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        packageName
    }

    private fun openApp(plan: NavigationLaunchPlan.OpenApp): NavigationOutcome {
        val intent = packageManager.getLaunchIntentForPackage(plan.packageName)
            ?: return NavigationOutcome.ACTIVITY_NOT_FOUND
        intent.addFlags(plan.flags)
        return start(intent)
    }

    private fun openView(plan: NavigationLaunchPlan.View, providerIsPinned: Boolean): NavigationOutcome {
        val intent = plan.attempts.asSequence()
            .map { spec ->
                Intent(Intent.ACTION_VIEW, Uri.parse(spec.uri)).apply {
                    spec.packageName?.let(::setPackage)
                    addFlags(spec.flags)
                }
            }
            .firstOrNull { it.resolveActivity(packageManager) != null }
            ?: return if (providerIsPinned) {
                NavigationOutcome.ACTIVITY_NOT_FOUND
            } else {
                NavigationOutcome.NO_PROVIDER
            }
        return start(intent)
    }

    private fun start(intent: Intent): NavigationOutcome = try {
        appContext.startActivity(intent)
        NavigationOutcome.REQUESTED
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        classifyNavigationLaunchFailure(failure)
    }
}
