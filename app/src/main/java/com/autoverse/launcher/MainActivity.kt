package com.autoverse.launcher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permission callback */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val fineLoc = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineLoc != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        // Check if Notification Listener is granted so media controls (Spotify, YouTube Music) work
        if (!com.autoverse.launcher.core.media.isNotificationAccessEnabled(this)) {
            val prefs = getSharedPreferences("autoverse_prefs", MODE_PRIVATE)
            val alreadyPrompted = prefs.getBoolean("has_prompted_notification_access", false)
            if (!alreadyPrompted) {
                prefs.edit().putBoolean("has_prompted_notification_access", true).apply()
                try {
                    startActivity(com.autoverse.launcher.core.media.notificationAccessSettingsIntent())
                } catch (_: Exception) {}
            }
        }

        setContent {
            AppRoot()
        }
    }
}
