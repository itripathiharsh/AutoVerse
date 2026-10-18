package com.autoverse.launcher.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.AutoVerseScreen
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.components.WidgetContainer
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.navigation.NavigationOutcome
import com.autoverse.launcher.core.navigation.navigationFailureMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Phase 7 — embedded-map proof of concept, reachable only while the
 * experimental "Embedded map" setting is on. The navigation hand-off stays
 * the default product behavior and is reused here as the fallback whenever
 * the map cannot render. The MapView exists only while this screen is
 * composed; leaving the screen destroys it.
 */
@Composable
fun MapPocScreen(
    providerLabel: String,
    onOpenNavigationApp: suspend () -> NavigationOutcome,
    onBack: () -> Unit,
    controller: MapPocController = remember { MapPocController() },
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var reloadToken by remember { mutableIntStateOf(0) }

    val onFallback: () -> Unit = {
        scope.launch {
            val outcome = onOpenNavigationApp()
            if (outcome != NavigationOutcome.OPENED) {
                snackbarHostState.showSnackbar(
                    message = navigationFailureMessage(providerLabel, outcome),
                    withDismissAction = true,
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        AutoVerseScreen(
            title = "Map preview",
            onBack = onBack,
            scrollable = false,
            modifier = Modifier.padding(innerPadding),
        ) {
            WidgetContainer(
                title = "EMBEDDED MAP \u2014 PROTOTYPE",
                subtitle = "MapLibre demo style. Evaluation only \u2014 not a production tile service.",
                icon = Icons.Outlined.Map,
                modifier = Modifier.weight(1f),
            ) {
                MapPocContent(
                    state = state,
                    providerLabel = providerLabel,
                    onRetry = {
                        reloadToken += 1
                        controller.reload()
                    },
                    onOpenNavigationApp = onFallback,
                    mapSurface = {
                        EmbeddedMapSurface(
                            controller = controller,
                            reloadToken = reloadToken,
                            modifier = Modifier.fillMaxSize(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Chrome for the prototype, with the map surface injected so tests can
 * replace it with a stub and exercise every state without native libraries.
 */
@Composable
fun MapPocContent(
    state: MapPocState,
    providerLabel: String,
    onRetry: () -> Unit,
    onOpenNavigationApp: () -> Unit,
    mapSurface: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.surfaceRaised),
        ) {
            if (state !is MapPocState.Unavailable) {
                mapSurface()
            }
            when (state) {
                MapPocState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
                    ) {
                        CircularProgressIndicator(color = colors.accent)
                        Text(
                            text = "Loading map\u2026",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }

                MapPocState.Ready -> Unit

                MapPocState.Empty -> MapPocMessage(
                    headline = "No map data",
                    message = "The map style loaded without any sources to draw.",
                    providerLabel = providerLabel,
                    showRetry = true,
                    onRetry = onRetry,
                    onOpenNavigationApp = onOpenNavigationApp,
                )

                is MapPocState.Error -> MapPocMessage(
                    headline = "Map couldn't load",
                    message = state.failure.message(),
                    providerLabel = providerLabel,
                    showRetry = true,
                    onRetry = onRetry,
                    onOpenNavigationApp = onOpenNavigationApp,
                )

                is MapPocState.Unavailable -> MapPocMessage(
                    headline = "Map renderer unavailable",
                    message = state.reason,
                    providerLabel = providerLabel,
                    showRetry = false,
                    onRetry = onRetry,
                    onOpenNavigationApp = onOpenNavigationApp,
                )
            }
        }
        Spacer(Modifier.size(AutoVerseSpacing.xs))
        Text(
            text = "Map data: MapLibre demo tiles. Prototype only \u2014 " +
                "production needs a licensed tile provider.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textFaint,
        )
    }
}

@Composable
private fun MapPocMessage(
    headline: String,
    message: String,
    providerLabel: String,
    showRetry: Boolean,
    onRetry: () -> Unit,
    onOpenNavigationApp: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.surfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(AutoVerseSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
            ) {
                PrimaryAction(
                    text = "Open in $providerLabel",
                    onClick = onOpenNavigationApp,
                )
                if (showRetry) {
                    TextAction(text = "Try again", onClick = onRetry)
                }
            }
        }
    }
}

/**
 * The real MapLibre-backed surface, shared by the map preview and the
 * navigation page (Phase 18). The native view is created once per screen
 * visit, forwards activity lifecycle to the map, and is destroyed on dispose
 * so no map work continues on other screens. Texture camera is a fixed demo
 * position; no location permission is requested.
 */
@Composable
fun EmbeddedMapSurface(
    controller: MapPocController,
    reloadToken: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val state by controller.state.collectAsStateWithLifecycle()
    val mapView = remember {
        runCatching { MapView(context).apply { onCreate(null) } }.getOrNull()
    }

    if (mapView == null) {
        LaunchedEffect(Unit) {
            controller.onSurfaceUnavailable("The map engine couldn't start on this device.")
        }
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView, reloadToken) {
        mapView.getMapAsync { map ->
            runCatching {
                map.cameraPosition = CameraPosition.Builder()
                    .target(
                        org.maplibre.android.geometry.LatLng(
                            MapPocConfig.START_LAT,
                            MapPocConfig.START_LON,
                        ),
                    )
                    .zoom(MapPocConfig.START_ZOOM)
                    .build()
                map.setStyle(Style.Builder().fromUri(MapPocConfig.STYLE_URL)) { style ->
                    controller.onStyleLoaded(style.sources.size)
                }
            }.onFailure {
                controller.onStyleLoadFailed(MapPocFailure.RUNTIME_FAILURE)
            }
        }
    }

    if (state is MapPocState.Loading) {
        LaunchedEffect(state, reloadToken) {
            delay(MapPocConfig.LOAD_TIMEOUT_MS)
            controller.onStyleLoadFailed(MapPocFailure.LOAD_TIMEOUT)
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
