package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons

/**
 * Destination entry shared by the map regions. Renders through
 * [AutoVerseTextField] so every surface keeps one styling source.
 */
@Composable
fun DestinationField(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    AutoVerseTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = "Where to?",
        modifier = modifier,
        isError = isError,
        leadingIcon = {
            Icon(
                Icons.Outlined.Place,
                contentDescription = null,
                modifier = Modifier.size(AutoVerseIcons.compact),
            )
        },
        trailingIcon = trailingIcon,
    )
}
