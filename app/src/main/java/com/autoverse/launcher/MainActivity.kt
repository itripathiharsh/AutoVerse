package com.autoverse.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-activity host. Permissions are never requested here: onboarding and
 * the panels ask in context, after explaining why, so a first launch opens
 * straight into the welcome screen instead of a system dialog.
 *
 * Also receives places from other apps: Google Maps "Share → AutoVerse",
 * geo: links and Google Maps links (WhatsApp locations etc.). The text is
 * handed to the app root, which navigates there on AutoVerse's own map.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleLocationIntent(intent)
        setContent {
            AppRoot()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLocationIntent(intent)
    }

    private fun handleLocationIntent(intent: Intent?) {
        sharedLocationText(intent)?.let { IncomingLocation.offer(it) }
    }
}

/** The text of a shared / opened location, or null for any other intent (e.g. HOME). */
internal fun sharedLocationText(intent: Intent?): String? = when (intent?.action) {
    Intent.ACTION_SEND -> listOfNotNull(
        intent.getStringExtra(Intent.EXTRA_SUBJECT),
        intent.getStringExtra(Intent.EXTRA_TEXT),
    ).joinToString("\n").takeIf { it.isNotBlank() }
    Intent.ACTION_VIEW -> intent.dataString?.takeIf { it.isNotBlank() }
    else -> null
}

/** One pending shared location, consumed by the app root once it is ready. */
object IncomingLocation {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun offer(text: String) {
        _pending.value = text
    }

    fun consume(): String? = _pending.value.also { _pending.value = null }
}
