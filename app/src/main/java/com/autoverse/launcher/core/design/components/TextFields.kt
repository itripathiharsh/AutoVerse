package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes

/**
 * Phase 18 — shared cockpit text field: single-line, tile-shaped, themed
 * colors. First extracted from the navigation card's destination field so
 * the navigation page (search, save form) reuses one implementation.
 */
@Composable
fun AutoVerseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val colors = LocalAutoVerseColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        placeholder = {
            Text(text = placeholder, style = MaterialTheme.typography.bodyLarge)
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = AutoVerseShapes.tile,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            errorTextColor = colors.textPrimary,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.outline,
            errorBorderColor = colors.danger,
            focusedPlaceholderColor = colors.textFaint,
            unfocusedPlaceholderColor = colors.textFaint,
            cursorColor = colors.accent,
            focusedLeadingIconColor = colors.textSecondary,
            unfocusedLeadingIconColor = colors.textFaint,
            errorLeadingIconColor = colors.danger,
            focusedTrailingIconColor = colors.textSecondary,
            unfocusedTrailingIconColor = colors.textFaint,
            errorTrailingIconColor = colors.danger,
        ),
    )
}
