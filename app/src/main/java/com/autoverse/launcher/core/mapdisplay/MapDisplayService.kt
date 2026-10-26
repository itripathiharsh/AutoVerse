package com.autoverse.launcher.core.mapdisplay

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.IBinder
import android.view.InputEvent
import android.view.MotionEvent
import android.view.Surface
import java.lang.reflect.Method
import kotlin.system.exitProcess

/**
 * Shizuku user service: runs inside the Shizuku server process with **shell
 * (uid 2000) privileges**, loaded from AutoVerse's APK. With those privileges
 * it does the three things scrcpy proved possible without root:
 *
 *  1. create a virtual display rendering into a surface AutoVerse provides
 *     (the SurfaceView inside our map region),
 *  2. host Google Maps on it (the app side launches Maps with
 *     `am start --display <id>` through the same shell privileges),
 *  3. inject the touches the user makes on our region into that display
 *     (`IInputManager.injectInputEvent` — shell is allowed to inject).
 *
 * Everything here is best-effort and fails loud: any refusal from the
 * platform throws, and the app-side probe logs it and falls back to the
 * keyless OSM map.
 */
class MapDisplayService(private val context: Context) : IMapsDisplayService.Stub() {

    private val displays = HashMap<Int, android.hardware.display.VirtualDisplay>()

    /** The system input manager binder proxy, resolved as shell. */
    private val inputManagerProxy: Any by lazy {
        val binder = Class
            .forName("android.os.ServiceManager")
            .getMethod("getService", String::class.java)
            .invoke(null, "input") as IBinder
        val stub = Class.forName("android.hardware.input.IInputManager\$Stub")
        val asInterface = stub.getDeclaredMethod("asInterface", IBinder::class.java)
        asInterface.invoke(null, binder)!!
    }

    /** Hidden `injectInputEvent` overloads, across the signature changes. */
    private val injectMethods: List<Method> by lazy {
        inputManagerProxy.javaClass.methods
            .filter { it.name == "injectInputEvent" }
            .onEach { it.isAccessible = true }
    }

    /** Hidden `InputEvent.setDisplayId` — present from API 30. */
    private val setDisplayIdMethod: Method? by lazy {
        try {
            InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
        } catch (_: Exception) {
            null // Pre-30: injected events follow focus; the probe logs this.
        }
    }

    @Synchronized
    override fun createDisplay(width: Int, height: Int, density: Int, surface: Surface): Int {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        // Flag sets tried in order: PUBLIC is the requirement (other apps may
        // render on the display); SUPPORTS_TOUCH (hidden, value 4) keeps
        // gestures working; OWN_CONTENT_ONLY stops unwanted mirroring.
        val attempts = listOf(
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
                FLAG_SUPPORTS_TOUCH,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY,
        )
        var lastFailure: Exception? = null
        for (flags in attempts) {
            try {
                val virtualDisplay = manager.createVirtualDisplay(
                    "autoverse_maps",
                    width, height, density, surface, flags,
                )
                val id = virtualDisplay.display.displayId
                displays[id] = virtualDisplay
                return id
            } catch (failure: Exception) {
                lastFailure = failure
            }
        }
        throw IllegalStateException(
            "Virtual display refused (tried ${attempts.size} flag sets): " +
                "${lastFailure?.message}",
        )
    }

    @Synchronized
    override fun injectMotionEvent(event: MotionEvent, displayId: Int) {
        setDisplayIdMethod?.invoke(event, displayId)
        var lastFailure: Exception? = null
        for (method in injectMethods) {
            try {
                when (method.parameterTypes.size) {
                    2 -> method.invoke(inputManagerProxy, event, MODE_ASYNC)
                    3 -> method.invoke(inputManagerProxy, event, MODE_ASYNC, 0)
                }
                return
            } catch (failure: Exception) {
                lastFailure = failure
            }
        }
        throw IllegalStateException("Input injection refused: ${lastFailure?.message}")
    }

    /** Executes as this service (shell uid); `am start --display` needs that. */
    override fun runShell(command: Array<String>): Int {
        val process = Runtime.getRuntime().exec(command)
        process.inputStream.readBytes()
        process.errorStream.readBytes()
        return process.waitFor()
    }

    @Synchronized
    override fun destroyDisplay(displayId: Int) {
        displays.remove(displayId)?.release()
    }

    @Synchronized
    override fun destroy() {
        displays.values.forEach { runCatching { it.release() } }
        displays.clear()
        exitProcess(0)
    }

    companion object {
        /** DisplayManager.VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH (hidden constant). */
        private const val FLAG_SUPPORTS_TOUCH = 1 shl 2

        /** InputManager.INJECT_INPUT_EVENT_MODE_ASYNC (hidden constant). */
        private const val MODE_ASYNC = 0
    }
}
