package com.autoverse.launcher.core.map

/**
 * 0.28.1 — read-only facts the map-embedding probe collects on the actual
 * device. Every field is a platform answer, never an assumption: the probe
 * screen reads these from Build / PackageManager / Settings and this module
 * turns them into a verdict. No hidden APIs, no reflection into the system.
 */
data class MapEmbedFacts(
    val sdkInt: Int,
    val versionRelease: String,
    val manufacturer: String,
    val model: String,
    val fingerprint: String,
    /** PackageManager.FEATURE_FREEFORM_WINDOW_MANAGEMENT. */
    val freeformSupported: Boolean,
    /** PackageManager.FEATURE_ACTIVITIES_ON_SECONDARY_DISPLAY. */
    val activitiesOnSecondaryDisplaySupported: Boolean,
    /** Google Maps installed and launchable. */
    val mapsInstalled: Boolean,
    val mapsVersionName: String?,
    /** ApplicationInfo.FLAG_RESIZEABLE_FOR_SCREENS on the Maps package. */
    val mapsResizeable: Boolean?,
    /** Developer option "Force activities to be resizable"; null when unreadable. */
    val forceResizableActivities: Boolean?,
)

/** The candidate approaches the investigation allows (no scraping, no SDK key). */
enum class EmbedApproach(val displayName: String) {
    ADJACENT_SPLIT("Split-screen adjacent launch"),
    FREEFORM_BOUNDS("Freeform window with launch bounds"),
    SAME_TASK_EMBEDDING("Activity embedding (same task)"),
}

/**
 * One approach's verdict. [viable] is true/false only where the platform
 * rules decide from [MapEmbedFacts]; null means the approach cannot be ruled
 * in or out without running the live probe on the device (for example,
 * split-screen adjacency depends on whether the system is already in
 * multi-window mode, which only a runtime probe can observe).
 */
data class ApproachAssessment(
    val approach: EmbedApproach,
    val viable: Boolean?,
    val explanation: String,
)

/**
 * Platform-rules verdicts per approach. These mirror the documented Android
 * semantics:
 *
 *  - Adjacent split launch (FLAG_ACTIVITY_LAUNCH_ADJACENT): a request that
 *    the system honours only while the device is already in split-screen
 *    mode, which the user enters manually. Proportions are system/user
 *    controlled — a launcher cannot command 60/20/20 or 70/30. Google Maps
 *    must also be resizeable for multi-window.
 *  - Freeform bounds: requires the FEATURE_FREEFORM_WINDOW_MANAGEMENT
 *    system feature. Even then, the HOME/launcher activity is not a freeform
 *    participant: when Maps' task comes forward, the launcher is covered,
 *    so this yields a resizable Maps window, not a persistent side-by-side.
 *  - Activity embedding: activities must open inside the embedding app's own
 *    task and the embedded app must opt in; Maps launches singleTask into its
 *    own task and does not opt in, so a third-party launcher cannot embed it.
 *    (Requires an Android 12L+ windowing shell in addition.)
 */
fun assessEmbeddingApproaches(facts: MapEmbedFacts): List<ApproachAssessment> = listOf(
    ApproachAssessment(
        approach = EmbedApproach.ADJACENT_SPLIT,
        viable = when {
            !facts.mapsInstalled -> false
            facts.mapsResizeable == false &&
                facts.forceResizableActivities != true -> false
            else -> null // decided by the live probe: is the device already split?
        },
        explanation = "The flag is honoured only while the device is already in " +
            "split-screen (user-entered) and the system picks the proportions; " +
            "60/20/20 and 70/30 layouts cannot be commanded. Run the adjacent " +
            "launch below and check whether Maps lands beside AutoVerse.",
    ),
    ApproachAssessment(
        approach = EmbedApproach.FREEFORM_BOUNDS,
        viable = facts.freeformSupported,
        explanation = "Launch bounds work only with freeform window management. " +
            "Even where present, the home app is not a freeform participant, so " +
            "Maps gains a resizable window but AutoVerse does not remain beside " +
            "it as a persistent 60/20/20 dock.",
    ),
    ApproachAssessment(
        approach = EmbedApproach.SAME_TASK_EMBEDDING,
        viable = false,
        explanation = "Activity embedding (Android 12L+) splits activities opened " +
            "into the embedding app's own task, and the embedded app must opt in. " +
            "Google Maps starts singleTask in its own task and does not opt in, " +
            "so a launcher cannot embed it. This device runs SDK " +
            "${facts.sdkInt} (12L needs 32+).",
    ),
)

/** The single-line conclusion the probe screen prints under the verdicts. */
fun overallRecommendation(facts: MapEmbedFacts, assessments: List<ApproachAssessment>): String {
    if (!facts.mapsInstalled) {
        return "Google Maps is not installed on this unit; the handoff surface " +
            "falls back to the system geo resolver."
    }
    if (assessments.any { it.approach == EmbedApproach.FREEFORM_BOUNDS && it.viable == true }) {
        return "Freeform window management is present. Run the bounds probe: if the " +
            "unit keeps AutoVerse visible beside the Maps window, the bounded " +
            "launch can serve as the map region; otherwise the handoff surface " +
            "remains the supported behavior."
    }
    return "No supported no-key path keeps Google Maps permanently visible inside " +
        "AutoVerse's regions on this device. The honest options are (a) the " +
        "existing handoff surface, (b) manually entering split-screen and using " +
        "the adjacent-launch probe, or (c) approving the Maps SDK (API key + " +
        "billing) documented in docs/map-integration.md."
}
