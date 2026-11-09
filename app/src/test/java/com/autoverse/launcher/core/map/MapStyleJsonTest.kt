package com.autoverse.launcher.core.map

import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapStyleJsonTest {

    private val liberty: String = File("src/main/assets/${MapStyle.STYLE_ASSET}").readText()

    private fun layer(style: JSONObject, id: String): JSONObject? {
        val layers = style.getJSONArray("layers")
        for (i in 0 until layers.length()) if (layers.getJSONObject(i).getString("id") == id) return layers.getJSONObject(i)
        return null
    }

    @Test
    fun `night style is dark, keeps the keyless OpenFreeMap tiles and drops the relief raster`() {
        val night = JSONObject(MapStyleJson.night(liberty))
        assertEquals("#10141A", layer(night, "background")!!.getJSONObject("paint").getString("background-color"))
        val sources = night.getJSONObject("sources")
        assertTrue(sources.getJSONObject("openmaptiles").getString("url").startsWith(MapStyle.TILE_HOST))
        assertFalse(sources.has("ne2_shaded"))
        assertNull(layer(night, "natural_earth"))
        assertFalse(night.toString().contains("key=", ignoreCase = true))
    }

    @Test
    fun `night roads have a hierarchy - highways warm, minor roads grey`() {
        val night = JSONObject(MapStyleJson.night(liberty))
        fun color(id: String) = layer(night, id)!!.getJSONObject("paint").getString("line-color")
        val motorway = color("road_motorway")
        val primary = color("road_trunk_primary")
        val minor = color("road_minor")
        assertNotEquals(motorway, minor)
        assertNotEquals(primary, minor)
        assertNotEquals(color("road_minor_casing"), minor)
        // Bridges and tunnels follow their road class.
        assertEquals(motorway, color("bridge_motorway"))
        assertEquals(motorway, color("tunnel_motorway"))
    }

    @Test
    fun `labels are recoloured with a dark halo and enlarged`() {
        val night = JSONObject(MapStyleJson.night(liberty))
        val road = layer(night, "highway-name-major")!!
        assertEquals("#0B0E13", road.getJSONObject("paint").getString("text-halo-color"))
        val size = road.getJSONObject("layout").getJSONArray("text-size")
        val stock = layer(JSONObject(liberty), "highway-name-major")!!.getJSONObject("layout").getJSONArray("text-size")
        assertTrue(size.getDouble(size.length() - 1) > stock.getDouble(stock.length() - 1))
    }

    @Test
    fun `day style is stock liberty without the relief raster`() {
        val day = JSONObject(MapStyleJson.day(liberty))
        assertNull(layer(day, "natural_earth"))
        assertEquals("#f8f4f0", layer(day, "background")!!.getJSONObject("paint").getString("background-color"))
    }

    @Test
    fun `lite mode hides only layers that exist in the style`() {
        val night = JSONObject(MapStyleJson.night(liberty))
        MapStyle.LITE_HIDDEN_LAYERS.forEach { assertNotNull("missing $it", layer(night, it)) }
    }

    @Test
    fun `size scaling covers literals and interpolate outputs, not zoom stops`() {
        assertEquals(15.0, MapStyleJson.scaleSize(12, 1.25) as Double, 0.0)
        val expr = JSONArray("""["interpolate",["linear"],["zoom"],13,12,14,13]""")
        val scaled = MapStyleJson.scaleSize(expr, 1.5) as JSONArray
        assertEquals(13, scaled.getInt(3))
        assertEquals(18.0, scaled.getDouble(4), 0.0)
        assertEquals(14, scaled.getInt(5))
        assertEquals(19.5, scaled.getDouble(6), 0.0)
    }
}
