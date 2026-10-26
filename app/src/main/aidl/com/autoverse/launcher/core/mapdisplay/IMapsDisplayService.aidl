package com.autoverse.launcher.core.mapdisplay;

import android.view.Surface;
import android.view.MotionEvent;

/**
 * Shizuku user service (runs with shell privileges) that hosts Google Maps
 * on a virtual display AutoVerse owns. Each method fails soft: callers get
 * exceptions they can log, never silent lies.
 */
interface IMapsDisplayService {

    /**
     * Creates a virtual display rendering into [surface]. Returns the
     * display id, or -1 with a thrown SecurityException when the platform
     * refuses. [density] is the display's DPI.
     */
    int createDisplay(int width, int height, int density, in Surface surface);

    /** Sends a full touch/gesture event to the display (display-targeted on API 30+). */
    void injectMotionEvent(in MotionEvent event, int displayId);

    /**
     * Runs a command with the service's own (shell) privileges and returns
     * its exit code — used for `am start --display <id> ...`, a documented
     * adb command shape.
     */
    int runShell(in String[] command);

    /** Releases one display; the activity on it is torn down by the system. */
    void destroyDisplay(int displayId);

    /** Releases everything; called by Shizuku on service teardown. */
    void destroy();
}
