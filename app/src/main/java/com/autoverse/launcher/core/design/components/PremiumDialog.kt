package com.autoverse.launcher.core.design.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.autoverse.launcher.R

/**
 * Theme entitlement dialog. 1.0 is a paid app with every theme included, so
 * the entitlement defaults to granted and this dialog only appears if it was
 * cleared (e.g. a settings reset from an older build); it restores access
 * without any payment. A free-with-upgrade model would put Play Billing here.
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
                "Every AutoVerse theme is included with your purchase \u2014 no " +
                    "account, no extra payment.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onUnlock) { Text("Unlock all themes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Maybe later") }
        },
    )
}
