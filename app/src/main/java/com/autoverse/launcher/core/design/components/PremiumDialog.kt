package com.autoverse.launcher.core.design.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.autoverse.launcher.R

/**
 * Premium entitlement dialog. Honest by design: this build collects no
 * payment — the button grants the local entitlement so every premium theme
 * unlocks, and says exactly that. When distribution starts, Play Billing
 * replaces the grant action behind the same flag.
 */
@Composable
fun PremiumUnlockDialog(
    onUnlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Premium themes") },
        text = {
            Text(
                "Sakura, Neon Tokyo, Shonen, Gotham and Hot Rod ship with " +
                    "AutoVerse Pro. This early-access build unlocks everything " +
                    "free \u2014 no account, no payment.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onUnlock) { Text("Unlock everything (early access)") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Maybe later") }
        },
    )
}
