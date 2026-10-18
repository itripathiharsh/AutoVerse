package com.autoverse.launcher.core.navigation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
private const val WAZE_PACKAGE = "com.waze"

/**
 * [NavigationRepository] over PackageManager and explicit launch intents.
 * Known providers are looked up by package; further apps discovered through
 * the geo: scheme become open-only targets. All intents leave as ACTION_VIEW
 * with FLAG_ACTIVITY_NEW_TASK from the application context.
 */
class SystemNavigationRepository(context: Context) : NavigationRepository {

    private val appContext = context.applicationContext
    private val selfPackage = appContext.packageName

    override suspend fun availableProviders(): List<NavigationProvider> =
        withContext(Dispatchers.IO) {
            val providers = mutableListOf<NavigationProvider>(NavigationProvider.SystemDefault)
            if (isInstalled(GOOGLE_MAPS_PACKAGE)) {
                providers += NavigationProvider.GoogleMaps
            }
            if (isInstalled(WAZE_PACKAGE)) {
                providers += NavigationProvider.Waze
            }
            providers += discoveredOpenOnlyProviders()
            providers
        }

    override suspend fun launch(
        destination: NavigationDestination?,
        provider: NavigationProvider,
    ): NavigationOutcome = withContext(Dispatchers.IO) {
        when {
            destination == null -> openApp(provider)
            !provider.supportsDestination -> NavigationOutcome.NOT_SUPPORTED
            else -> openDestination(provider, destination)
        }
    }

    private fun isInstalled(packageName: String): Boolean = try {
        appContext.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun discoveredOpenOnlyProviders(): List<NavigationProvider> {
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
        return try {
            appContext.packageManager.queryIntentActivities(geoIntent, 0)
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
        val info = appContext.packageManager.getApplicationInfo(packageName, 0)
        appContext.packageManager.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        packageName
    }

    private fun openDestination(
        provider: NavigationProvider,
        destination: NavigationDestination,
    ): NavigationOutcome {
        val uri = navigationDestinationUri(provider, destination)
            ?: return NavigationOutcome.NOT_SUPPORTED
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        provider.packageName?.let { intent.setPackage(it) }
        return try {
            appContext.startActivity(intent)
            NavigationOutcome.OPENED
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            val outcome = classifyNavigationLaunchFailure(failure)
            if (outcome == NavigationOutcome.ACTIVITY_NOT_FOUND && provider.packageName == null) {
                NavigationOutcome.NO_PROVIDER
            } else {
                outcome
            }
        }
    }

    private fun openApp(provider: NavigationProvider): NavigationOutcome {
        val packageName = provider.packageName ?: return NavigationOutcome.NOT_SUPPORTED
        val intent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return NavigationOutcome.ACTIVITY_NOT_FOUND
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(intent)
            NavigationOutcome.OPENED
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            classifyNavigationLaunchFailure(failure)
        }
    }
}
