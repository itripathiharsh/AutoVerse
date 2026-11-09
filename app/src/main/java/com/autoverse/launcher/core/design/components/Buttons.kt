package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseBorders
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/** What a filled action means: the one main action, or ending/removing something. */
enum class ActionTone { ACCENT, DANGER }

/** Filled action with comfortable car-scale touch height; one per panel at most. */
@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: ActionTone = ActionTone.ACCENT,
    icon: ImageVector? = null,
) {
    val colors = LocalAutoVerseColors.current
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = AutoVerseSpacing.touchComfort),
        enabled = enabled,
        shape = AutoVerseShapes.pill,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (tone == ActionTone.DANGER) colors.danger else colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabledContent,
        ),
        contentPadding = PaddingValues(
            horizontal = AutoVerseSpacing.lg,
            vertical = AutoVerseSpacing.sm,
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(AutoVerseIcons.standard))
            androidx.compose.foundation.layout.Spacer(Modifier.size(AutoVerseSpacing.xs))
        }
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

enum class TransportVariant { PRIMARY, SECONDARY }

/**
 * Circular media/transport control. PRIMARY is the accent-filled lead action
 * (64dp); SECONDARY is an outlined outline-surface control (52dp). Pressed and
 * disabled states are explicit; both variants clear the 48dp touch minimum.
 */
@Composable
fun TransportButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TransportVariant = TransportVariant.SECONDARY,
    enabled: Boolean = true,
) {
    val colors = LocalAutoVerseColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val buttonSize = if (variant == TransportVariant.PRIMARY) {
        AutoVerseSpacing.transportPrimary
    } else {
        AutoVerseSpacing.transportSecondary
    }
    val iconSize = if (variant == TransportVariant.PRIMARY) {
        AutoVerseIcons.prominent
    } else {
        AutoVerseIcons.standard
    }

    val containerColor = when {
        !enabled -> colors.disabledContainer
        pressed && variant == TransportVariant.PRIMARY -> lerp(colors.accent, colors.onAccent, 0.15f)
        pressed -> colors.surfaceHighlight
        variant == TransportVariant.PRIMARY -> colors.accent
        else -> colors.surfaceHighlight
    }
    val contentColor = if (enabled) {
        if (variant == TransportVariant.PRIMARY) colors.onAccent else colors.textPrimary
    } else {
        colors.disabledContent
    }

    Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = contentColor,
        )
    }
}

/**
 * Borderless text action for low-emphasis operations (e.g. "Change").
 * [quiet] draws it in the neutral text colour, for secondary actions that
 * sit next to a filled primary one, so only one thing on a panel is blue.
 */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    quiet: Boolean = false,
) {
    val colors = LocalAutoVerseColors.current
    val tint = when {
        !enabled -> colors.disabledContent
        quiet -> colors.textPrimary
        else -> colors.accent
    }
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .defaultMinSize(minHeight = AutoVerseSpacing.touchMin)
            .clip(AutoVerseShapes.pill)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            ) { onClick() }
            .padding(horizontal = AutoVerseSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
        )
    }
}
