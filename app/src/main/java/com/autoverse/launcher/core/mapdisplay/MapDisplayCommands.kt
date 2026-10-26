package com.autoverse.launcher.core.mapdisplay

import android.content.Intent

/**
 * Pure builders for the shell side of the display trick. Everything is a
 * documented `adb shell` command shape, executed with shell privileges
 * through Shizuku.
 */
object MapDisplayCommands {

    const val MAPS_PACKAGE = "com.google.android.apps.maps"

    /**
     * Launches Google Maps on [displayId]: `am start --display <id> -a
     * VIEW -d <geo-uri> -p com.google.android.apps.maps`. The geo URI comes
     * from the caller (a typed destination or a bare `geo:0,0`).
     */
    fun launchMapsOnDisplay(displayId: Int, geoUri: String = "geo:0,0"): Array<String> =
        arrayOf(
            "am", "start",
            "--display", displayId.toString(),
            "-a", Intent.ACTION_VIEW,
            "-d", geoUri,
            "-p", MAPS_PACKAGE,
        )

    /**
     * Fallback launch (Maps not pinned): the platform geo resolver picks the
     * navigation app on the display.
     */
    fun launchGeoOnDisplay(displayId: Int, geoUri: String = "geo:0,0"): Array<String> =
        arrayOf(
            "am", "start",
            "--display", displayId.toString(),
            "-a", Intent.ACTION_VIEW,
            "-d", geoUri,
        )
}

/**
 * Pure view→display coordinate mapping. The virtual display is rendered into
 * a (possibly scaled) SurfaceView, so touches must be rescaled by the same
 * factor and clamped inside the display before injection.
 */
object TouchMapper {

    fun scaleX(viewX: Float, viewWidth: Float, displayWidth: Int): Float =
        map(viewX, viewWidth, displayWidth)

    fun scaleY(viewY: Float, viewHeight: Float, displayHeight: Int): Float =
        map(viewY, viewHeight, displayHeight)

    fun isInside(viewX: Float, viewY: Float, viewWidth: Float, viewHeight: Float): Boolean =
        viewX in 0f..viewWidth && viewY in 0f..viewHeight

    private fun map(viewCoord: Float, viewSize: Float, displaySize: Int): Float {
        if (viewSize <= 0f || displaySize <= 0) return 0f
        return (viewCoord * (displaySize / viewSize)).coerceIn(0f, displaySize.toFloat())
    }
}
