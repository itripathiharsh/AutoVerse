package com.autoverse.launcher.core.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Location
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentConstants
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.autoverse.launcher.core.location.SpeedometerData
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponent
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.OnCameraTrackingChangedListener
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory

/**
 * Owns the single MapLibre [MapView] for the activity. Home and Maps both
 * show this one instance (it is re-parented, never rebuilt), so switching
 * screens costs no GL context, style parse, or tile reload — the map is
 * simply there. Lifecycle follows the activity, so rendering stops whenever
 * AutoVerse is not visible.
 *
 * Location comes from AutoVerse's own GPS feed (once a second) and is handed
 * to the location component, which interpolates the puck and camera at the
 * map's frame rate — smooth motion without high-frequency GPS. Updates with
 * no real movement are dropped so a parked car does not keep the GPU busy.
 */
class LiveMapHost(
    context: Context,
    private val lifecycle: Lifecycle,
    backgroundColor: Int,
) : DefaultLifecycleObserver {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var lastRememberedMs = 0L
    private val frames = MapFrameAccumulator()
    private var map: MapLibreMap? = null
    private var loadedStyleUrl: String? = null
    private var requestedStyleUrl: String? = null
    private var pendingLocation: Location? = null
    private var lastPushed: Location? = null
    private var destroyed = false

    /** True after the user panned away; the UI then offers "Recenter". */
    var trackingDismissed by mutableStateOf(false)
        private set

    /** False until a style finished loading; the UI shows a quiet placeholder until then. */
    var styleReady by mutableStateOf(false)
        private set

    val mapView: MapView

    init {
        MapLibre.getInstance(appContext)
        // Storage-aware: up to 200 MB, but never more than 5% of the unit's free space.
        val freeBytes = try {
            android.os.StatFs(appContext.filesDir.path).availableBytes
        } catch (_: Exception) {
            MapStyle.AMBIENT_CACHE_BYTES * 20
        }
        OfflineManager.getInstance(appContext).setMaximumAmbientCacheSize(
            com.autoverse.launcher.core.car.mapCacheBytesFor(freeBytes),
            object : OfflineManager.FileSourceCallback {
                override fun onSuccess() = Unit
                override fun onError(message: String) = Unit
            },
        )
        val options = MapLibreMapOptions.createFromAttributes(context)
            .textureMode(false) // SurfaceView: its own composition layer, cheapest path on weak GPUs
            .attributionEnabled(false) // the panel shows the required attribution itself
            .logoEnabled(false)
            .compassEnabled(false)
            .foregroundLoadColor(backgroundColor)
        mapView = MapView(context, options)
        mapView.onCreate(null)
        mapView.addOnDidFinishRenderingFrameListener { _, _, _ ->
            frames.onFrame(SystemClock.uptimeMillis())?.let(MapRenderStats::publish)
        }
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.uiSettings.apply {
                setTiltGesturesEnabled(false) // pitched views cost more tiles and fill-rate
                setCompassEnabled(false)
                setLogoEnabled(false)
                setAttributionEnabled(false)
            }
            // Last remembered car position, else an India overview — never 0,0.
            val (lat, lng, zoom) = MapStyle.startCamera(
                prefs.getString(KEY_LAT, null)?.toDoubleOrNull(),
                prefs.getString(KEY_LNG, null)?.toDoubleOrNull(),
            )
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(lat, lng))
                .zoom(zoom)
                .build()
            requestedStyleUrl?.let(::loadStyle)
        }
        lifecycle.addObserver(this)
    }

    fun setNight(night: Boolean) {
        val url = MapStyle.styleUrl(night)
        if (url == requestedStyleUrl) return
        requestedStyleUrl = url
        if (map != null) loadStyle(url)
    }

    fun setMaxFps(fps: Int) = mapView.setMaximumFps(fps)

    private var lite = false

    /** Lite mode hides layers that cost frame time without helping a driver. */
    fun setLite(enabled: Boolean) {
        if (enabled == lite) return
        lite = enabled
        map?.style?.let(::applyLite)
    }

    private fun applyLite(style: Style) {
        val visibility = PropertyFactory.visibility(if (lite) Property.NONE else Property.VISIBLE)
        MapStyle.LITE_HIDDEN_LAYERS.forEach { id -> style.getLayer(id)?.setProperties(visibility) }
    }

    /** Feeds a GPS fix; ignored until it represents real movement. */
    fun updateLocation(data: SpeedometerData) {
        val lat = data.latitude ?: return
        val lng = data.longitude ?: return
        val location = Location("autoverse").apply {
            latitude = lat
            longitude = lng
            data.bearingDegrees?.let { bearing = it }
            data.currentSpeedKmh?.let { speed = it / 3.6f }
            data.accuracyMeters?.let { accuracy = it }
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        val previous = lastPushed
        if (previous != null && !isMeaningfulMove(previous, location)) return
        val component = activeComponent()
        if (component == null) {
            pendingLocation = location
            return
        }
        push(component, location, data.currentSpeedKmh)
    }

    /** Re-engages follow mode after the user panned or zoomed away. */
    fun recenter() = followCar()

    private fun loadStyle(url: String) {
        val readyMap = map ?: return
        if (url == loadedStyleUrl) return
        styleReady = false
        readyMap.setStyle(Style.Builder().fromUri(url)) { style ->
            loadedStyleUrl = url
            applyLite(style)
            styleReady = true
            activateLocation(readyMap, style)
            applyRoute(style) // a day/night switch reloads the style; keep the route
        }
    }

    // ---- route line (in-app navigation) -------------------------------------

    private var routeLine: List<LatLng>? = null
    private var destination: LatLng? = null

    /** Shows (or with null, clears) the route polyline and destination pin. */
    fun setRoute(points: List<LatLng>?) {
        routeLine = points?.takeIf { it.size >= 2 }
        destination = routeLine?.last()
        map?.style?.takeIf { styleReady }?.let(::applyRoute)
    }

    private fun applyRoute(style: Style) {
        val line = routeLine
        val lineGeo = line?.let {
            LineString.fromLngLats(it.map { p -> Point.fromLngLat(p.longitude, p.latitude) })
        }
        val pinGeo = destination?.let { Point.fromLngLat(it.longitude, it.latitude) }

        val lineSource = style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)
        if (lineSource == null) {
            if (lineGeo == null) return
            style.addSource(GeoJsonSource(ROUTE_SOURCE, lineGeo))
            style.addSource(
                if (pinGeo != null) GeoJsonSource(DEST_SOURCE, pinGeo) else GeoJsonSource(DEST_SOURCE),
            )
            val casing = LineLayer(ROUTE_CASING_LAYER, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#0B3D91")),
                PropertyFactory.lineWidth(11f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
            val route = LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(Color.parseColor("#2F7BFF")),
                PropertyFactory.lineWidth(7f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            )
            val pin = CircleLayer(DEST_LAYER, DEST_SOURCE).withProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(Color.parseColor("#E53935")),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleStrokeWidth(3f),
            )
            // Under the car puck, above the roads.
            val below = LocationComponentConstants.SHADOW_LAYER
            if (style.getLayer(below) != null) {
                style.addLayerBelow(casing, below)
                style.addLayerBelow(route, below)
                style.addLayerBelow(pin, below)
            } else {
                style.addLayer(casing)
                style.addLayer(route)
                style.addLayer(pin)
            }
        } else {
            val empty = org.maplibre.geojson.FeatureCollection.fromFeatures(emptyList<org.maplibre.geojson.Feature>())
            if (lineGeo != null) lineSource.setGeoJson(lineGeo) else lineSource.setGeoJson(empty)
            style.getSourceAs<GeoJsonSource>(DEST_SOURCE)?.let { pinSource ->
                if (pinGeo != null) pinSource.setGeoJson(pinGeo) else pinSource.setGeoJson(empty)
            }
        }
    }

    /**
     * Frames the whole route (preview); the user can recenter afterwards.
     * [bottomPx] keeps the route clear of the preview card at the bottom.
     */
    fun showOverview(points: List<LatLng>, paddingPx: Int, bottomPx: Int) {
        val readyMap = map ?: return
        if (points.size < 2) return
        activeComponent()?.cameraMode = CameraMode.NONE
        trackingDismissed = true
        val bounds = LatLngBounds.Builder().includes(points).build()
        readyMap.easeCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, paddingPx, paddingPx, paddingPx, bottomPx),
            600,
        )
    }

    /**
     * Driving view for turn-by-turn: heading-up, close zoom, following the
     * car. The zoom is part of the camera-mode transition — a separate
     * zoomWhileTracking() is ignored while that transition is still running.
     */
    fun followForGuidance() {
        val component = activeComponent() ?: return
        trackingDismissed = false
        component.setCameraMode(CameraMode.TRACKING_GPS, 750L, MapStyle.GUIDANCE_ZOOM, null, null, null)
    }

    /** Back to following the car at normal driving zoom (navigation ended). */
    fun followCar() {
        val component = activeComponent() ?: return
        trackingDismissed = false
        val mode = cameraModeFor(lastPushed?.speed?.times(3.6f)?.toInt())
        component.setCameraMode(mode, 750L, MapStyle.FOLLOW_ZOOM, null, null, null)
    }

    @SuppressLint("MissingPermission") // fed by AutoVerse's own permission-checked GPS; no engine here
    private fun activateLocation(readyMap: MapLibreMap, style: Style) {
        val component = readyMap.locationComponent
        component.activateLocationComponent(
            LocationComponentActivationOptions.Builder(appContext, style)
                .useDefaultLocationEngine(false)
                .build(),
        )
        component.isLocationComponentEnabled = true
        component.setMaxAnimationFps(MapStyle.LOCATION_ANIMATION_MAX_FPS)
        component.renderMode = RenderMode.NORMAL
        component.cameraMode = CameraMode.TRACKING
        component.addOnCameraTrackingChangedListener(object : OnCameraTrackingChangedListener {
            override fun onCameraTrackingDismissed() {
                trackingDismissed = true
            }

            override fun onCameraTrackingChanged(currentMode: Int) {
                if (currentMode != CameraMode.NONE) trackingDismissed = false
            }
        })
        val first = pendingLocation ?: lastPushed
        pendingLocation = null
        if (first != null) {
            push(component, first, first.speed.times(3.6f).toInt())
            component.zoomWhileTracking(MapStyle.FOLLOW_ZOOM)
        }
    }

    private fun push(component: LocationComponent, location: Location, speedKmh: Int?) {
        val firstFix = lastPushed == null
        component.forceLocationUpdate(location)
        lastPushed = location
        rememberPosition(location)
        if (!trackingDismissed) {
            val wanted = cameraModeFor(speedKmh)
            if (component.cameraMode != wanted) component.cameraMode = wanted
            val moving = MapStyle.rotateWithHeading(speedKmh)
            component.renderMode = if (moving) RenderMode.GPS else RenderMode.NORMAL
        }
        if (firstFix) component.zoomWhileTracking(MapStyle.FOLLOW_ZOOM)
    }

    /** Keeps the car's position (on this unit only) so the next start opens there. */
    private fun rememberPosition(location: Location) {
        val now = SystemClock.elapsedRealtime()
        if (lastRememberedMs != 0L && now - lastRememberedMs < MapStyle.REMEMBER_POSITION_EVERY_MS) return
        lastRememberedMs = now
        prefs.edit()
            .putString(KEY_LAT, location.latitude.toString())
            .putString(KEY_LNG, location.longitude.toString())
            .apply()
    }

    private fun activeComponent(): LocationComponent? {
        val readyMap = map ?: return null
        val component = readyMap.locationComponent
        return if (styleReady && component.isLocationComponentActivated) component else null
    }

    private fun cameraModeFor(speedKmh: Int?): Int =
        if (MapStyle.rotateWithHeading(speedKmh)) CameraMode.TRACKING_GPS else CameraMode.TRACKING

    override fun onStart(owner: LifecycleOwner) {
        if (!destroyed) mapView.onStart()
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!destroyed) mapView.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        if (!destroyed) mapView.onPause()
    }

    override fun onStop(owner: LifecycleOwner) {
        if (!destroyed) mapView.onStop()
    }

    override fun onDestroy(owner: LifecycleOwner) = release()

    fun onLowMemory() {
        if (!destroyed) mapView.onLowMemory()
    }

    /** Idempotent teardown; safe from both the lifecycle and composition disposal. */
    fun release() {
        if (destroyed) return
        destroyed = true
        lifecycle.removeObserver(this)
        mapView.onDestroy()
        map = null
    }
}

/** Pure movement filter: below these deltas a new fix would only redraw the same frame. */
internal fun isMeaningfulMove(previous: Location, next: Location): Boolean =
    isMeaningfulMove(
        distanceMeters = previous.distanceTo(next),
        bearingDelta = bearingDelta(previous.bearing, next.bearing),
    )

fun isMeaningfulMove(distanceMeters: Float, bearingDelta: Float): Boolean =
    distanceMeters >= MIN_MOVE_METERS || bearingDelta >= MIN_BEARING_CHANGE_DEG

fun bearingDelta(a: Float, b: Float): Float {
    val d = kotlin.math.abs(a - b) % 360f
    return if (d > 180f) 360f - d else d
}

private const val ROUTE_SOURCE = "autoverse-route"
private const val DEST_SOURCE = "autoverse-destination"
private const val ROUTE_LAYER = "autoverse-route-line"
private const val ROUTE_CASING_LAYER = "autoverse-route-casing"
private const val DEST_LAYER = "autoverse-destination-pin"

private const val PREFS = "autoverse_map"
private const val KEY_LAT = "last_lat"
private const val KEY_LNG = "last_lng"

const val MIN_MOVE_METERS = 1.5f
const val MIN_BEARING_CHANGE_DEG = 3f
