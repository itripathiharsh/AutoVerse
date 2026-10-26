package com.autoverse.launcher.core.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
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
        OfflineManager.getInstance(appContext).setMaximumAmbientCacheSize(
            MapStyle.AMBIENT_CACHE_BYTES,
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
            readyMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(0.0, 0.0))
                .zoom(MapStyle.NO_FIX_ZOOM)
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
    fun recenter() {
        val component = activeComponent() ?: return
        component.cameraMode = cameraModeFor(lastPushed?.speed?.times(3.6f)?.toInt())
        component.zoomWhileTracking(MapStyle.FOLLOW_ZOOM)
        trackingDismissed = false
    }

    private fun loadStyle(url: String) {
        val readyMap = map ?: return
        if (url == loadedStyleUrl) return
        styleReady = false
        readyMap.setStyle(Style.Builder().fromUri(url)) { style ->
            loadedStyleUrl = url
            styleReady = true
            activateLocation(readyMap, style)
        }
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
        if (!trackingDismissed) {
            val wanted = cameraModeFor(speedKmh)
            if (component.cameraMode != wanted) component.cameraMode = wanted
            val moving = MapStyle.rotateWithHeading(speedKmh)
            component.renderMode = if (moving) RenderMode.GPS else RenderMode.NORMAL
        }
        if (firstFix) component.zoomWhileTracking(MapStyle.FOLLOW_ZOOM)
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

const val MIN_MOVE_METERS = 1.5f
const val MIN_BEARING_CHANGE_DEG = 3f
