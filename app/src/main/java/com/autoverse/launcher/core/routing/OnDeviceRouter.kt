package com.autoverse.launcher.core.routing

import btools.router.FormatJson
import btools.router.OsmNodeNamed
import btools.router.RoutingContext
import btools.router.RoutingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Why a route could not be produced, in words a driver understands. */
class RoutingException(message: String) : Exception(message)

/**
 * Computes car routes on the head unit with the embedded BRouter engine.
 * Runs on a background thread (the UI never waits on it) and can be
 * cancelled — leaving navigation or starting a new route stops the old
 * calculation at once.
 */
class OnDeviceRouter(
    private val segmentDir: java.io.File,
    private val profilePath: () -> String,
) {
    constructor(data: RoutingDataStore) : this(data.segmentDir, data::carProfilePath)

    suspend fun route(from: GeoPoint, to: GeoPoint): Route = withContext(Dispatchers.Default) {
        val profile = profilePath()
        val rc = RoutingContext().apply { localFunction = profile }
        val waypoints = mutableListOf(node(from, "from"), node(to, "to"))
        val engine = RoutingEngine(null, null, segmentDir, waypoints, rc)
        engine.quite = true

        val json = suspendCancellableCoroutine<String?> { cont ->
            cont.invokeOnCancellation { engine.terminate() }
            engine.doRun(MAX_ROUTING_MS)
            val track = engine.foundTrack
            cont.resume(track?.let { FormatJson(rc).format(it) })
        }
        val error = engine.errorMessage
        if (json == null) throw RoutingException(friendlyError(error))
        parseBRouterJson(json).also {
            if (it.points.size < 2) throw RoutingException("No road route found to that place")
        }
    }

    private fun node(p: GeoPoint, name: String) = OsmNodeNamed().apply {
        this.name = name
        ilon = ((p.longitude + 180.0) * 1_000_000.0).roundToInt()
        ilat = ((p.latitude + 90.0) * 1_000_000.0).roundToInt()
    }

    companion object {
        /** Long cross-state routes on slow units get this long before we give up. */
        const val MAX_ROUTING_MS = 60_000L

        fun friendlyError(raw: String?): String = when {
            raw == null -> "Couldn't calculate a route"
            raw.contains("not found", ignoreCase = true) && raw.contains("datafile", ignoreCase = true) ->
                "Road data for this area isn't downloaded yet"
            raw.contains("position not mapped", ignoreCase = true) || raw.contains("from-position", ignoreCase = true) ->
                "You're not near a mapped road"
            raw.contains("to-position", ignoreCase = true) ->
                "That destination isn't near a road"
            raw.contains("timeout", ignoreCase = true) || raw.contains("terminated", ignoreCase = true) ->
                "Route took too long to calculate — try a closer destination"
            else -> "Couldn't calculate a route"
        }
    }
}
