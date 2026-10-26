package com.autoverse.launcher.core.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapEmbedProbeTest {

    private fun facts(
        sdkInt: Int = 33,
        freeform: Boolean = false,
        mapsInstalled: Boolean = true,
        mapsResizeable: Boolean? = true,
        forceResizable: Boolean? = false,
    ) = MapEmbedFacts(
        sdkInt = sdkInt,
        versionRelease = "13",
        manufacturer = "Sample",
        model = "Unit",
        fingerprint = "sample/unit:13",
        freeformSupported = freeform,
        activitiesOnSecondaryDisplaySupported = false,
        mapsInstalled = mapsInstalled,
        mapsVersionName = "1.0",
        mapsResizeable = mapsResizeable,
        forceResizableActivities = forceResizable,
    )

    private fun assessment(
        facts: MapEmbedFacts,
        approach: EmbedApproach,
    ): ApproachAssessment = assessEmbeddingApproaches(facts).first { it.approach == approach }

    @Test
    fun `same-task embedding is always ruled out for Maps`() {
        val result = assessment(facts(sdkInt = 34), EmbedApproach.SAME_TASK_EMBEDDING)
        assertEquals(false, result.viable)
    }

    @Test
    fun `adjacent split stays a live-probe question on a normal unit`() {
        assertNull(assessment(facts(), EmbedApproach.ADJACENT_SPLIT).viable)
    }

    @Test
    fun `adjacent split is ruled out when maps is missing`() {
        assertEquals(
            false,
            assessment(facts(mapsInstalled = false), EmbedApproach.ADJACENT_SPLIT).viable,
        )
    }

    @Test
    fun `adjacent split is ruled out when maps refuses resizing`() {
        assertEquals(
            false,
            assessment(
                facts(mapsResizeable = false, forceResizable = false),
                EmbedApproach.ADJACENT_SPLIT,
            ).viable,
        )
    }

    @Test
    fun `forced resizable activities reopens the adjacent split question`() {
        assertNull(
            assessment(
                facts(mapsResizeable = false, forceResizable = true),
                EmbedApproach.ADJACENT_SPLIT,
            ).viable,
        )
    }

    @Test
    fun `freeform bounds verdict follows the system feature`() {
        assertTrue(assessment(facts(freeform = true), EmbedApproach.FREEFORM_BOUNDS).viable!!)
        assertFalse(assessment(facts(freeform = false), EmbedApproach.FREEFORM_BOUNDS).viable!!)
    }

    @Test
    fun `recommendation names the handoff when nothing supports embedding`() {
        val result = assessEmbeddingApproaches(facts())
        assertTrue(
            overallRecommendation(facts(), result)
                .contains("handoff"),
        )
    }

    @Test
    fun `recommendation explains a missing maps install`() {
        val result = assessEmbeddingApproaches(facts(mapsInstalled = false))
        assertTrue(
            overallRecommendation(facts(mapsInstalled = false), result)
                .contains("not installed"),
        )
    }

    @Test
    fun `recommendation points at the bounds probe on freeform units`() {
        val result = assessEmbeddingApproaches(facts(freeform = true))
        assertTrue(
            overallRecommendation(facts(freeform = true), result)
                .contains("bounds probe"),
        )
    }
}
