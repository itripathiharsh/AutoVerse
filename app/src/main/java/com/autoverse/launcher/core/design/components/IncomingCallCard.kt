package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.PhoneInTalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.phone.CallUiState
import com.autoverse.launcher.core.phone.callDisplayNumber

/**
 * The Home incoming/active-call panel, shown in place of the speed summary
 * only while the platform reports a call. Informational by design: answering
 * stays with the system phone UI (programmatic answer needs privileged or
 * deprecated APIs), so there are no accept/reject controls to fake.
 */
@Composable
fun IncomingCallCard(
    callState: CallUiState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val colors = LocalAutoVerseColors.current
    val incoming = callState is CallUiState.Incoming
    val number = callDisplayNumber(
        when (callState) {
            is CallUiState.Incoming -> callState.number
            is CallUiState.InCall -> callState.number
            CallUiState.Idle -> null
        },
    )

    WidgetContainer(
        title = if (incoming) "INCOMING CALL" else "ON A CALL",
        icon = Icons.Outlined.Call,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AutoVerseSpacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.PhoneInTalk,
                contentDescription = null,
                modifier = Modifier.size(if (compact) AutoVerseSpacing.iconChip else AutoVerseSpacing.heroChip),
                tint = if (incoming) colors.accent else colors.textSecondary,
            )
            Spacer(Modifier.size(AutoVerseSpacing.sm))
            Text(
                text = number ?: "Number not provided",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(AutoVerseSpacing.xxs))
            Text(
                text = if (incoming) "Incoming call \u2014 answer from the phone UI"
                else "Call in progress",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!compact) Spacer(Modifier.size(AutoVerseSpacing.xs))
            if (!compact) Text(
                text = "The Speedometer panel returns when the call ends.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textFaint,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
