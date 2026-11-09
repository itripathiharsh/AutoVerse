package com.autoverse.launcher.core.map

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds AutoVerse's map styles from OpenFreeMap's "liberty" style, bundled
 * in assets (pinned, 43 KB, parsed once): full road classes with casings,
 * bridges, tunnels, POIs and shields — the hierarchy the old "dark" style
 * lacked. The night variant is recoloured here so it renders correctly from
 * the first frame (no flash of daylight colours, no runtime paint calls).
 *
 * Night palette, designed for dark cabins: slate land, blue-tinted water,
 * green parks, a clear grey ramp for minor → secondary roads and warm amber
 * for primary roads and highways (as drivers expect from premium night
 * maps), with larger, haloed labels. Pure and JVM-tested.
 */
object MapStyleJson {

    /** Day: stock liberty without the shaded-relief raster (costly, adds nothing at street zoom). */
    fun day(liberty: String): String = JSONObject(liberty).apply { dropRelief(this) }.toString()

    fun night(liberty: String): String {
        val style = JSONObject(liberty)
        dropRelief(style)
        val layers = style.getJSONArray("layers")
        val keep = JSONArray()
        for (i in 0 until layers.length()) {
            val layer = layers.getJSONObject(i)
            val id = layer.getString("id")
            if (id in NIGHT_DROPPED) continue
            recolor(id, layer)
            keep.put(layer)
        }
        style.put("layers", keep)
        return style.toString()
    }

    private fun dropRelief(style: JSONObject) {
        style.optJSONObject("sources")?.remove("ne2_shaded")
        val layers = style.getJSONArray("layers")
        val keep = JSONArray()
        for (i in 0 until layers.length()) {
            val layer = layers.getJSONObject(i)
            if (layer.optString("source") != "ne2_shaded") keep.put(layer)
        }
        style.put("layers", keep)
    }

    private fun recolor(id: String, layer: JSONObject) {
        val paint = layer.optJSONObject("paint") ?: JSONObject().also { layer.put("paint", it) }
        val type = layer.optString("type")
        fun line(color: String) = paint.put("line-color", color)
        when {
            id == "background" -> paint.put("background-color", N.LAND)

            id == "park" -> {
                paint.put("fill-color", N.PARK); paint.put("fill-opacity", 0.9)
                paint.remove("fill-outline-color")
            }
            id == "park_outline" -> paint.put("line-opacity", 0)
            id == "landuse_residential" -> { paint.put("fill-color", N.RESIDENTIAL); paint.put("fill-opacity", 1) }
            id == "landcover_wood" -> { paint.put("fill-color", N.WOOD); paint.put("fill-opacity", 0.7) }
            id == "landcover_grass" || id == "landuse_pitch" || id == "landuse_track" ->
                { paint.put("fill-color", N.GRASS); paint.put("fill-opacity", 0.6) }
            id == "landcover_wetland" -> { paint.put("fill-color", N.WOOD); paint.put("fill-opacity", 0.5) }
            id == "landcover_ice" -> { paint.put("fill-color", N.ICE); paint.put("fill-opacity", 0.6) }
            id == "landcover_sand" -> { paint.put("fill-color", N.SAND); paint.put("fill-opacity", 0.6) }
            id == "landuse_cemetery" -> { paint.put("fill-color", N.GRASS); paint.put("fill-opacity", 0.4) }
            id == "landuse_hospital" -> { paint.put("fill-color", N.HOSPITAL); paint.put("fill-opacity", 0.5) }
            id == "landuse_school" -> { paint.put("fill-color", N.SCHOOL); paint.put("fill-opacity", 0.5) }

            id == "water" -> { paint.put("fill-color", N.WATER); paint.remove("fill-outline-color") }
            id.startsWith("waterway") && type == "line" -> line(N.WATERWAY)

            id == "aeroway_fill" -> paint.put("fill-color", N.AEROWAY)
            id.startsWith("aeroway_") && type == "line" -> line(N.RUNWAY)

            id == "building" -> {
                paint.put("fill-color", N.BUILDING); paint.put("fill-outline-color", N.BUILDING_EDGE)
            }
            id == "building-3d" -> {
                paint.put("fill-extrusion-color", N.BUILDING_3D); paint.put("fill-extrusion-opacity", 0.6)
            }

            type == "line" && id.endsWith("_hatching") -> line(N.LAND)
            type == "line" && id.endsWith("_rail") -> line(N.RAIL)
            type == "line" && isRoad(id) -> {
                val casing = id.endsWith("_casing")
                val cls = roadClass(id)
                line(if (casing) cls.casing else cls.fill)
            }

            id.startsWith("boundary") -> line(N.BOUNDARY)

            type == "symbol" -> recolorLabel(id, layer, paint)
        }
    }

    private fun recolorLabel(id: String, layer: JSONObject, paint: JSONObject) {
        val layout = layer.optJSONObject("layout")
        val (color, scale) = when {
            id.startsWith("highway-name-major") -> N.ROAD_LABEL_MAJOR to 1.3
            id.startsWith("highway-name") -> N.ROAD_LABEL to 1.3
            id.startsWith("highway-shield") || id.startsWith("road_shield") -> return
            id.startsWith("road_one_way") -> { paint.put("icon-opacity", 0.35); return }
            id.startsWith("water") -> N.WATER_LABEL to 1.15
            id.startsWith("poi") || id == "airport" -> {
                paint.put("icon-opacity", 0.8)
                N.POI_LABEL to 1.15
            }
            id == "label_city" || id == "label_city_capital" || id.startsWith("label_country") ||
                id == "label_state" -> N.PLACE_MAJOR to 1.2
            id == "label_town" -> N.PLACE_MAJOR to 1.2
            id.startsWith("label") -> N.PLACE_MINOR to 1.2
            else -> N.POI_LABEL to 1.0
        }
        paint.put("text-color", color)
        paint.put("text-halo-color", N.HALO)
        paint.put("text-halo-width", 1.6)
        paint.put("text-halo-blur", 0.4)
        layout?.opt("text-size")?.let { layout.put("text-size", scaleSize(it, scale)) }
    }

    /** Scales a literal size or the outputs of an ["interpolate", …] / {"stops"} size. */
    internal fun scaleSize(value: Any, factor: Double): Any = when (value) {
        is Number -> round1(value.toDouble() * factor)
        is JSONArray -> if (value.optString(0) == "interpolate") {
            JSONArray().also { out ->
                for (i in 0 until value.length()) {
                    val v = value.get(i)
                    out.put(if (i >= 4 && i % 2 == 0 && v is Number) round1(v.toDouble() * factor) else v)
                }
            }
        } else {
            value
        }
        is JSONObject -> value.optJSONArray("stops")?.let { stops ->
            val out = JSONArray()
            for (i in 0 until stops.length()) {
                val stop = stops.getJSONArray(i)
                out.put(JSONArray().put(stop.get(0)).put(round1(stop.getDouble(1) * factor)))
            }
            JSONObject(value.toString()).put("stops", out)
        } ?: value
        else -> value
    }

    private fun round1(v: Double): Double = Math.round(v * 10.0) / 10.0

    private fun isRoad(id: String): Boolean =
        id.startsWith("road_") || id.startsWith("tunnel_") || id.startsWith("bridge_")

    private class RoadColors(val fill: String, val casing: String)

    private fun roadClass(id: String): RoadColors = when {
        "motorway_link" in id -> RoadColors(N.MOTORWAY_LINK, N.MOTORWAY_CASING)
        "motorway" in id -> RoadColors(N.MOTORWAY, N.MOTORWAY_CASING)
        "trunk_primary" in id -> RoadColors(N.PRIMARY, N.PRIMARY_CASING)
        "secondary_tertiary" in id || "_link" in id -> RoadColors(N.SECONDARY, N.SECONDARY_CASING)
        "path_pedestrian" in id -> RoadColors(N.PATH, N.MINOR_CASING)
        "service_track" in id -> RoadColors(N.SERVICE, N.MINOR_CASING)
        else -> RoadColors(N.MINOR, N.MINOR_CASING) // minor, street
    }

    /** Patterned area fills read as noise at night. */
    private val NIGHT_DROPPED = setOf("road_area_pattern")

    /** Night palette. */
    private object N {
        const val LAND = "#10141A"
        const val RESIDENTIAL = "#13181F"
        const val PARK = "#122A21"
        const val WOOD = "#11241D"
        const val GRASS = "#132820"
        const val ICE = "#1A2028"
        const val SAND = "#1E1C17"
        const val HOSPITAL = "#2A1A1E"
        const val SCHOOL = "#222019"
        const val WATER = "#0D2238"
        const val WATERWAY = "#123452"
        const val AEROWAY = "#181D25"
        const val RUNWAY = "#2A303A"
        const val BUILDING = "#1A1F27"
        const val BUILDING_EDGE = "#232933"
        const val BUILDING_3D = "#1E242D"
        const val RAIL = "#333A47"
        const val BOUNDARY = "#4A5060"

        const val PATH = "#272E38"
        const val SERVICE = "#2A313C"
        const val MINOR = "#333B48"
        const val MINOR_CASING = "#0C0F14"
        const val SECONDARY = "#4E5970"
        const val SECONDARY_CASING = "#151A22"
        const val PRIMARY = "#8A7550"
        const val PRIMARY_CASING = "#2A2217"
        const val MOTORWAY = "#C0853D"
        const val MOTORWAY_LINK = "#9C6D36"
        const val MOTORWAY_CASING = "#3A2914"

        const val ROAD_LABEL = "#C3CBD6"
        const val ROAD_LABEL_MAJOR = "#DDE3EB"
        const val WATER_LABEL = "#6A98CC"
        const val POI_LABEL = "#9AA6B8"
        const val PLACE_MAJOR = "#EEF1F5"
        const val PLACE_MINOR = "#A9B3C1"
        const val HALO = "#0B0E13"
    }
}
