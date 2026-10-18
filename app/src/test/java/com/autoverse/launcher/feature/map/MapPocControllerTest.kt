package com.autoverse.launcher.feature.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 7 — MapPocController transitions: every prototype state is reachable,
 * late successes recover from a timeout, stale failures never clobber a
 * loaded map, and the hand-off fallback is recommended exactly when the map
 * cannot render.
 */
class MapPocControllerTest {

    private val controller = MapPocController()

    @Test
    fun `starts in loading`() {
        assertEquals(MapPocState.Loading, controller.state.value)
    }

    @Test
    fun `style with sources becomes ready`() {
        controller.onStyleLoaded(sourceCount = 3)
        assertEquals(MapPocState.Ready, controller.state.value)
    }

    @Test
    fun `style without sources becomes empty with fallback shown`() {
        controller.onStyleLoaded(sourceCount = 0)
        assertEquals(MapPocState.Empty, controller.state.value)
        assertTrue(controller.state.value.showsFallback)
    }

    @Test
    fun `style failure while loading becomes a retryable error`() {
        controller.onStyleLoadFailed()
        assertEquals(MapPocState.Error(MapPocFailure.STYLE_LOAD_FAILED), controller.state.value)
        assertTrue(controller.state.value.showsFallback)
    }

    @Test
    fun `timeout while loading becomes a timeout error`() {
        controller.onStyleLoadFailed(MapPocFailure.LOAD_TIMEOUT)
        assertEquals(MapPocState.Error(MapPocFailure.LOAD_TIMEOUT), controller.state.value)
    }

    @Test
    fun `a late style load recovers a timeout error`() {
        controller.onStyleLoadFailed(MapPocFailure.LOAD_TIMEOUT)
        controller.onStyleLoaded(sourceCount = 1)
        assertEquals(MapPocState.Ready, controller.state.value)
    }

    @Test
    fun `stale style failure after ready is ignored`() {
        controller.onStyleLoaded(sourceCount = 2)
        controller.onStyleLoadFailed()
        assertEquals(MapPocState.Ready, controller.state.value)
    }

    @Test
    fun `runtime failure after ready becomes an error`() {
        controller.onStyleLoaded(sourceCount = 1)
        controller.onRuntimeFailure()
        assertEquals(MapPocState.Error(MapPocFailure.RUNTIME_FAILURE), controller.state.value)
    }

    @Test
    fun `reload from error returns to loading`() {
        controller.onStyleLoadFailed()
        controller.reload()
        assertEquals(MapPocState.Loading, controller.state.value)
    }

    @Test
    fun `reload from empty returns to loading and can reach ready`() {
        controller.onStyleLoaded(sourceCount = 0)
        controller.reload()
        assertEquals(MapPocState.Loading, controller.state.value)
        controller.onStyleLoaded(sourceCount = 1)
        assertEquals(MapPocState.Ready, controller.state.value)
    }

    @Test
    fun `reload while ready is ignored`() {
        controller.onStyleLoaded(sourceCount = 1)
        controller.reload()
        assertEquals(MapPocState.Ready, controller.state.value)
    }

    @Test
    fun `surface failure is terminal and shows fallback`() {
        controller.onSurfaceUnavailable("no renderer")
        assertEquals(MapPocState.Unavailable("no renderer"), controller.state.value)
        assertTrue(controller.state.value.showsFallback)

        controller.onStyleLoaded(sourceCount = 2)
        controller.onStyleLoadFailed()
        controller.onRuntimeFailure()
        controller.reload()
        assertEquals(MapPocState.Unavailable("no renderer"), controller.state.value)
    }

    @Test
    fun `loading and ready do not recommend the fallback`() {
        assertFalse(controller.state.value.showsFallback)
        controller.onStyleLoaded(sourceCount = 1)
        assertFalse(controller.state.value.showsFallback)
    }
}
