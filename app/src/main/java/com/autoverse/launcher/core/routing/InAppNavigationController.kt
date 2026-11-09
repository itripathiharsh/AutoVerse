package com.autoverse.launcher.core.routing

import android.app.Application
import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoverse.launcher.core.location.GpsStatus
import com.autoverse.launcher.core.location.SpeedometerData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Where in-app navigation stands. */
sealed interface NavState {
    data object Idle : NavState

    /** Road data downloading ([download] set, first time in an area) or the route calculating. */
    data class Preparing(val place: NavPlace, val download: TileDownloadProgress?) : NavState

    /** Route ready: distance and time shown, waiting for "Start". */
    data class Preview(val place: NavPlace, val route: Route) : NavState

    data class Guiding(
        val place: NavPlace,
        val route: Route,
        val progress: RouteProgress?,
        val rerouting: Boolean = false,
    ) : NavState

    data class Arrived(val place: NavPlace) : NavState

    /** New drive while a trip was unfinished: "Continue to X?". */
    data class ResumeOffer(val place: NavPlace) : NavState

    data class Failed(val place: NavPlace, val message: String) : NavState
}

/** Address search state for the "Where to?" field. */
data class SearchState(
    val query: String = "",
    val results: List<NavPlace> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

/** "Stops on the way" panel state. */
data class StopsState(
    val category: PoiCategory? = null,
    val loading: Boolean = false,
    val stops: List<RouteStop> = emptyList(),
    val error: String? = null,
)

/** The in-app navigation controller, provided once at the app root. */
val LocalInAppNavigation = staticCompositionLocalOf<InAppNavigationController?> { null }

/**
 * Turn-by-turn navigation inside AutoVerse: search → route preview → guided
 * drive with voice, live progress and automatic rerouting. Routes are
 * computed on the head unit (BRouter); road data for an area is downloaded
 * once over the car's internet and kept. Google Maps stays available as an
 * optional fallback in the UI, never the default.
 */
class InAppNavigationController(app: Application) : AndroidViewModel(app) {
    private val data = RoutingDataStore(app)
    private val router = OnDeviceRouter(data)
    private val search = PlaceSearch()
    private val poi = OverpassPoiSearch()
    private val voice = VoiceGuide(app)
    private val prefs = app.getSharedPreferences("autoverse_nav", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<NavState>(NavState.Idle)
    val state: StateFlow<NavState> = _state.asStateFlow()

    private val _search = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _search.asStateFlow()

    private val _recents = MutableStateFlow(loadRecents())
    val recents: StateFlow<List<NavPlace>> = _recents.asStateFlow()

    private val _voiceOn = MutableStateFlow(prefs.getBoolean(KEY_VOICE, true))
    val voiceOn: StateFlow<Boolean> = _voiceOn.asStateFlow()

    /** Speed limit of the current road (route when guiding, road lookup otherwise). */
    private val _speedLimit = MutableStateFlow<Int?>(null)
    val speedLimit: StateFlow<Int?> = _speedLimit.asStateFlow()

    /** Speed-limit chime/voice and camera warnings; on by default. */
    private val _alertsOn = MutableStateFlow(prefs.getBoolean(KEY_ALERTS, true))
    val alertsOn: StateFlow<Boolean> = _alertsOn.asStateFlow()

    private var lastPosition: GeoPoint? = null
    private var lastSpeedKmh: Int? = null
    private var lastLookupAt: GeoPoint? = null
    private var lastLookupMs = 0L
    private var lookupJob: Job? = null
    private var pendingLimit: Int? = null
    private var overCount = 0
    private var lastOverAlertMs = 0L
    private val warnedCameras = mutableSetOf<Int>()
    private val autoDownloadTried = mutableSetOf<String>()
    private val chime by lazy {
        try {
            android.media.ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 85)
        } catch (_: Exception) {
            null
        }
    }
    private var searchJob: Job? = null
    private var routeJob: Job? = null
    private var offRouteCount = 0
    private val announced = mutableSetOf<Pair<Int, PromptStage>>()

    // ---- search -------------------------------------------------------------

    fun setQuery(query: String) {
        _search.value = _search.value.copy(query = query, error = null)
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < PlaceSearch.MIN_QUERY_LENGTH) {
            _search.value = _search.value.copy(results = localMatches(trimmed), loading = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS) // fair use of the free search server
            _search.value = _search.value.copy(loading = true, results = localMatches(trimmed))
            try {
                val online = search.search(trimmed, lastPosition)
                val merged = (localMatches(trimmed) + online).distinctBy { it.name to it.detail }
                _search.value = _search.value.copy(results = merged, loading = false, error = null)
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                _search.value = _search.value.copy(
                    loading = false,
                    error = "Search needs internet — check the car's Wi-Fi",
                )
            }
        }
    }

    private fun localMatches(q: String): List<NavPlace> =
        if (q.isEmpty()) emptyList() else _recents.value.filter {
            it.name.contains(q, ignoreCase = true) || it.detail.contains(q, ignoreCase = true)
        }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchState()
    }

    // ---- routing ------------------------------------------------------------

    /** Picks a destination and computes the route preview. */
    fun navigateTo(place: NavPlace) {
        clearSearch()
        remember(place)
        computeRoute(place, startGuidingWhenDone = false)
    }

    fun retry() {
        val place = when (val s = _state.value) {
            is NavState.Failed -> s.place
            is NavState.Preview -> s.place
            else -> return
        }
        computeRoute(place, startGuidingWhenDone = false)
    }

    private fun computeRoute(place: NavPlace, startGuidingWhenDone: Boolean) {
        routeJob?.cancel()
        val from = lastPosition
        if (from == null) {
            _state.value = NavState.Failed(place, "Waiting for your location — make sure GPS is on, then tap Retry")
            return
        }
        routeJob = viewModelScope.launch {
            try {
                val needed = routingTilesBetween(from, place.point)
                if (needed.size > MAX_ROUTE_TILES) {
                    _state.value = NavState.Failed(place, "That's too far for one route — pick a closer stop first")
                    return@launch
                }
                val missing = data.missingTiles(needed)
                if (missing.isNotEmpty()) {
                    _state.value = NavState.Preparing(place, TileDownloadProgress(missing.first(), 0, missing.size, 0, 0))
                    data.download(missing) { p -> _state.value = NavState.Preparing(place, p) }
                }
                _state.value = NavState.Preparing(place, null)
                val route = router.route(from, place.point)
                if (startGuidingWhenDone) {
                    beginGuiding(place, route, spoken = NavText.routeUpdated(spokenLang()))
                } else {
                    _state.value = NavState.Preview(place, route)
                }
            } catch (c: CancellationException) {
                throw c
            } catch (e: RoutingException) {
                _state.value = NavState.Failed(place, e.message ?: "Couldn't calculate a route")
            } catch (_: java.io.IOException) {
                _state.value = NavState.Failed(
                    place,
                    "Couldn't download road data for this area — check the car's internet and tap Retry",
                )
            } catch (_: Exception) {
                _state.value = NavState.Failed(place, "Couldn't calculate a route")
            }
        }
    }

    fun startGuidance() {
        val s = _state.value as? NavState.Preview ?: return
        beginGuiding(s.place, s.route, spoken = NavText.starting(s.place.name, spokenLang()))
    }

    private fun beginGuiding(place: NavPlace, route: Route, spoken: String) {
        offRouteCount = 0
        announced.clear()
        warnedCameras.clear()
        val progress = lastPosition?.let { computeProgress(route, it) }
        _state.value = NavState.Guiding(place, route, progress)
        rememberActiveTrip(finalDestination ?: place)
        say(spoken)
    }

    // ---- unfinished trip & Home/Work -------------------------------------------

    private fun rememberActiveTrip(place: NavPlace?) {
        val e = prefs.edit()
        if (place == null) {
            e.remove(KEY_ACTIVE).remove(KEY_ACTIVE_MS)
        } else {
            e.putString(KEY_ACTIVE, placeJson(place).toString()).putLong(KEY_ACTIVE_MS, System.currentTimeMillis())
        }
        e.apply()
    }

    /** New drive: offer to continue a trip left unfinished in the last few hours. */
    fun offerResume() {
        if (_state.value !is NavState.Idle) return
        val saved = prefs.getString(KEY_ACTIVE, null) ?: return
        val at = prefs.getLong(KEY_ACTIVE_MS, 0L)
        if (System.currentTimeMillis() - at > RESUME_WITHIN_MS) {
            rememberActiveTrip(null)
            return
        }
        val place = try { placeFromJson(JSONObject(saved)) } catch (_: Exception) { null } ?: return
        _state.value = NavState.ResumeOffer(place)
    }

    fun acceptResume() {
        val s = _state.value as? NavState.ResumeOffer ?: return
        computeRoute(s.place, startGuidingWhenDone = true)
    }

    fun declineResume() {
        rememberActiveTrip(null)
        _state.value = NavState.Idle
    }

    private val _favorites = MutableStateFlow(
        mapOf(
            "home" to prefs.getString(KEY_HOME, null)?.let { runCatching { placeFromJson(JSONObject(it)) }.getOrNull() },
            "work" to prefs.getString(KEY_WORK, null)?.let { runCatching { placeFromJson(JSONObject(it)) }.getOrNull() },
        ),
    )
    /** "home" / "work" → place, when set. */
    val favorites: StateFlow<Map<String, NavPlace?>> = _favorites.asStateFlow()

    fun setFavorite(key: String, place: NavPlace) {
        _favorites.value = _favorites.value + (key to place)
        prefs.edit().putString(if (key == "home") KEY_HOME else KEY_WORK, placeJson(place).toString()).apply()
    }

    private fun placeJson(p: NavPlace) = JSONObject().put("n", p.name).put("d", p.detail).put("lat", p.latitude).put("lon", p.longitude)
    private fun placeFromJson(o: JSONObject) = NavPlace(o.getString("n"), o.optString("d"), o.getDouble("lat"), o.getDouble("lon"))

    /** Ends navigation (End button, Cancel on a preview or error, Done after arriving). */
    fun stop() {
        routeJob?.cancel()
        voice.stop()
        finalDestination = null
        closeStops()
        rememberActiveTrip(null)
        _state.value = NavState.Idle
    }

    // ---- stops on the way (petrol, CNG, EV, toilets, food) -------------------

    /** The destination to resume after a stop added during guidance. */
    private var finalDestination: NavPlace? = null

    private val _stops = MutableStateFlow(StopsState())
    val stopsState: StateFlow<StopsState> = _stops.asStateFlow()
    private var stopsJob: Job? = null

    /**
     * While guiding: searches a few points ahead on the route and keeps places
     * close to it. Otherwise: places near the car.
     */
    fun findStops(category: PoiCategory) {
        val here = lastPosition ?: run {
            _stops.value = StopsState(category, error = "Waiting for your location")
            return
        }
        stopsJob?.cancel()
        _stops.value = StopsState(category, loading = true)
        stopsJob = viewModelScope.launch {
            try {
                val guiding = _state.value as? NavState.Guiding
                val traveled = guiding?.progress?.traveledMeters ?: 0.0
                fun toStops(found: List<NavPlace>): List<RouteStop> = if (guiding != null) {
                    stopsAlongRoute(guiding.route, traveled, found)
                } else {
                    found.map { RouteStop(it, distanceMeters(here, it.point), 0.0) }.sortedBy { it.aheadMeters }
                }
                // 1) Fast name search first, so the driver sees answers in a second or two.
                val quick = try {
                    if (guiding != null) {
                        samplePointsAhead(guiding.route, traveled).flatMap { search.searchCategory(category, it, limit = 6) }
                    } else {
                        search.searchCategory(category, here, limit = 10)
                    }
                } catch (c: CancellationException) {
                    throw c
                } catch (_: Exception) {
                    emptyList()
                }
                if (quick.isNotEmpty()) _stops.value = StopsState(category, loading = true, stops = toStops(quick).take(12))
                // 2) Then every mapped place by tag (Overpass), merged in when it arrives.
                val tagged = try {
                    kotlinx.coroutines.withTimeoutOrNull(OVERPASS_WAIT_MS) {
                        if (guiding != null) poi.alongLine(category, routeLineAhead(guiding.route, traveled)) else poi.near(category, here)
                    }.orEmpty()
                } catch (c: CancellationException) {
                    throw c
                } catch (_: Exception) {
                    emptyList()
                }
                val merged = mergePlaces(tagged, quick)
                val stops = toStops(merged)
                _stops.value = StopsState(category, stops = stops.take(12), error = if (stops.isEmpty()) "None found nearby" else null)
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                _stops.value = StopsState(category, error = "Search needs internet — check the car's Wi-Fi")
            }
        }
    }

    fun closeStops() {
        stopsJob?.cancel()
        _stops.value = StopsState()
    }

    /** Adds [place] as a stop: guide there now, then continue to the destination. */
    fun addStop(place: NavPlace) {
        closeStops()
        val guiding = _state.value as? NavState.Guiding
        if (guiding == null) {
            navigateTo(place)
            return
        }
        finalDestination = finalDestination ?: guiding.place
        _state.value = guiding.copy(place = place, rerouting = true)
        reroute(place)
    }

    fun toggleVoice() {
        val on = !_voiceOn.value
        _voiceOn.value = on
        prefs.edit().putBoolean(KEY_VOICE, on).apply()
        if (!on) voice.stop()
    }

    // ---- live tracking ------------------------------------------------------

    /** Fed with every GPS update from the app root. */
    fun onLocation(data: SpeedometerData) {
        val lat = data.latitude ?: return
        val lon = data.longitude ?: return
        val position = GeoPoint(lat, lon)
        lastPosition = position
        lastSpeedKmh = data.currentSpeedKmh
        maybeAutoDownload(position)
        val s = _state.value as? NavState.Guiding
        if (s == null) {
            if (data.gpsStatus == GpsStatus.LIVE) lookUpSpeedLimit(position)
            checkOverSpeed()
            return
        }
        if (s.rerouting || data.gpsStatus != GpsStatus.LIVE) return

        val progress = computeProgress(s.route, position, s.progress?.segmentIndex ?: -1)
        _speedLimit.value = progress.speedLimitKmh
        checkOverSpeed()
        progress.nextCameraMeters?.let { d ->
            val cameraIndex = s.route.cameras.firstOrNull { s.route.cumulativeMeters[it] > progress.traveledMeters }
            if (cameraIndex != null && d <= cameraWarningMeters(lastSpeedKmh) && warnedCameras.add(cameraIndex)) {
                alert(NavText.camera(progress.speedLimitKmh, spokenLang()))
            }
        }
        if (progress.arrived) {
            val next = finalDestination
            if (next != null) {
                // Reached a stop on the way: carry on to the real destination.
                finalDestination = null
                say(NavText.arrived(s.place.name, spokenLang()))
                _state.value = s.copy(place = next, rerouting = true)
                reroute(next)
                return
            }
            _state.value = NavState.Arrived(s.place)
            rememberActiveTrip(null)
            say(NavText.arrived(s.place.name, spokenLang()))
            return
        }
        offRouteCount = if (progress.offRouteMeters > OFF_ROUTE_METERS) offRouteCount + 1 else 0
        if (offRouteCount >= OFF_ROUTE_FIXES_BEFORE_REROUTE) {
            offRouteCount = 0
            _state.value = s.copy(progress = progress, rerouting = true)
            say(NavText.recalculating(spokenLang()))
            reroute(s.place)
            return
        }
        _state.value = s.copy(progress = progress)
        announce(progress)
    }

    private fun reroute(place: NavPlace) {
        routeJob?.cancel()
        val from = lastPosition ?: return
        routeJob = viewModelScope.launch {
            try {
                val route = router.route(from, place.point)
                beginGuiding(place, route, spoken = NavText.routeUpdated(spokenLang()))
            } catch (c: CancellationException) {
                throw c
            } catch (_: Exception) {
                // Keep guiding on the old route; the next off-route streak retries.
                (_state.value as? NavState.Guiding)?.let { _state.value = it.copy(rerouting = false) }
            }
        }
    }

    private fun announce(progress: RouteProgress) {
        val next = progress.nextManeuver ?: return
        if (next.type == ManeuverType.ARRIVE && progress.distanceToNextManeuver > 300) return
        val stage = promptStageFor(progress.distanceToNextManeuver, lastSpeedKmh) ?: return
        if (!announced.add(next.pointIndex to stage)) return
        if (stage == PromptStage.NEAR) announced.add(next.pointIndex to PromptStage.FAR)
        val lang = spokenLang()
        say(
            when (stage) {
                PromptStage.FAR -> NavText.farPrompt(progress.distanceToNextManeuver, next, lang)
                PromptStage.NEAR -> NavText.nearPrompt(next, progress.thenManeuver, lang)
            },
        )
    }

    private fun say(text: String) {
        if (_voiceOn.value) voice.speak(text)
    }

    // ---- language ------------------------------------------------------------

    private val _language = MutableStateFlow(
        NavLanguage.entries.firstOrNull { it.name == prefs.getString(KEY_LANGUAGE, null) } ?: NavLanguage.ENGLISH,
    )
    /** Language of on-screen directions and (when the voice is installed) spoken ones. */
    val language: StateFlow<NavLanguage> = _language.asStateFlow()

    fun setLanguage(lang: NavLanguage) {
        _language.value = lang
        prefs.edit().putString(KEY_LANGUAGE, lang.name).apply()
        voice.setLanguage(lang)
    }

    /** Hindi is spoken only when the unit has a Hindi voice; otherwise English. */
    private fun spokenLang(): NavLanguage =
        if (_language.value == NavLanguage.HINDI && voice.hindiAvailable) NavLanguage.HINDI else NavLanguage.ENGLISH

    val hindiVoiceAvailable: Boolean get() = voice.hindiAvailable

    init {
        voice.setLanguage(_language.value)
    }

    // ---- speed limit & cameras ---------------------------------------------

    /**
     * Free driving: look up the road under the car every few seconds (≈20 ms
     * on the road data already on the unit). A new limit is shown only once
     * two lookups agree, so passing a side road never flickers the sign.
     */
    private fun lookUpSpeedLimit(position: GeoPoint) {
        val now = System.currentTimeMillis()
        val moved = lastLookupAt?.let { distanceMeters(it, position) } ?: Double.MAX_VALUE
        if (lookupJob?.isActive == true || (now - lastLookupMs < LOOKUP_EVERY_MS && moved < LOOKUP_EVERY_M)) return
        if (!data.hasTile(routingTileName(position.latitude, position.longitude))) {
            _speedLimit.value = null
            return
        }
        lastLookupAt = position
        lastLookupMs = now
        lookupJob = viewModelScope.launch {
            val limit = router.roadTagsAt(position)?.let(::maxspeedFromTags)?.takeIf { it > 0 }
            if (limit == _speedLimit.value) {
                pendingLimit = null
            } else if (limit == pendingLimit || _speedLimit.value == null) {
                _speedLimit.value = limit
                pendingLimit = null
            } else {
                pendingLimit = limit
            }
        }
    }

    /** Over the limit for 3 fixes in a row → chime + "Speed limit 50", at most once a minute. */
    private fun checkOverSpeed() {
        if (!isOverLimit(lastSpeedKmh, _speedLimit.value)) {
            overCount = 0
            return
        }
        overCount++
        val now = System.currentTimeMillis()
        if (overCount >= 3 && now - lastOverAlertMs > OVER_ALERT_EVERY_MS) {
            lastOverAlertMs = now
            _speedLimit.value?.let { alert(NavText.speedLimit(it, spokenLang())) }
        }
    }

    private fun alert(spoken: String) {
        if (!_alertsOn.value) return
        try {
            chime?.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 300)
        } catch (_: Exception) {
        }
        if (_voiceOn.value) voice.speak(spoken)
    }

    fun setAlertsOn(on: Boolean) {
        _alertsOn.value = on
        prefs.edit().putBoolean(KEY_ALERTS, on).apply()
    }

    /**
     * Downloads this area's road data in the background the first time the
     * car is there — only on an unmetered connection (car Wi-Fi), once per
     * area — so the speed-limit sign and navigation are ready.
     */
    private fun maybeAutoDownload(position: GeoPoint) {
        val tile = routingTileName(position.latitude, position.longitude)
        if (data.hasTile(tile) || !autoDownloadTried.add(tile)) return
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val unmetered = try {
            cm?.activeNetwork != null && cm.isActiveNetworkMetered.not()
        } catch (_: Exception) {
            false
        }
        if (!unmetered) {
            autoDownloadTried.remove(tile) // try again when on Wi-Fi
            return
        }
        viewModelScope.launch {
            try {
                data.download(listOf(tile)) { }
            } catch (_: Exception) {
                autoDownloadTried.remove(tile)
            }
        }
    }

    // ---- recent places (on this unit only) ---------------------------------

    private fun remember(place: NavPlace) {
        val updated = (listOf(place) + _recents.value.filterNot { it.name == place.name && it.detail == place.detail })
            .take(MAX_RECENTS)
        _recents.value = updated
        val arr = JSONArray()
        updated.forEach {
            arr.put(JSONObject().put("n", it.name).put("d", it.detail).put("lat", it.latitude).put("lon", it.longitude))
        }
        prefs.edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    private fun loadRecents(): List<NavPlace> = try {
        val arr = JSONArray(prefs.getString(KEY_RECENTS, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            NavPlace(o.getString("n"), o.optString("d"), o.getDouble("lat"), o.getDouble("lon"))
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun removeRecent(place: NavPlace) {
        val updated = _recents.value.filterNot { it == place }
        _recents.value = updated
        val arr = JSONArray()
        updated.forEach {
            arr.put(JSONObject().put("n", it.name).put("d", it.detail).put("lat", it.latitude).put("lon", it.longitude))
        }
        prefs.edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    override fun onCleared() {
        runCatching { chime?.release() }
        voice.shutdown()
        super.onCleared()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 450L
        const val MAX_RECENTS = 8
        const val KEY_RECENTS = "recent_places"
        const val KEY_VOICE = "voice_on"
        const val KEY_ALERTS = "alerts_on"
        const val KEY_LANGUAGE = "language"
        const val KEY_ACTIVE = "active_trip"
        const val KEY_ACTIVE_MS = "active_trip_ms"
        const val KEY_HOME = "fav_home"
        const val KEY_WORK = "fav_work"
        const val RESUME_WITHIN_MS = 4 * 3_600_000L
        const val OVERPASS_WAIT_MS = 45_000L
        const val LOOKUP_EVERY_MS = 6_000L
        const val LOOKUP_EVERY_M = 80.0
        const val OVER_ALERT_EVERY_MS = 60_000L
    }
}
