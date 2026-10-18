package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseIcons
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.navigation.DestinationParseResult
import com.autoverse.launcher.core.navigation.NavigationDestination
import com.autoverse.launcher.core.navigation.ResolvedNavigationProvider
import com.autoverse.launcher.core.navigation.destinationFailureMessage
import com.autoverse.launcher.core.navigation.parseNavigationDestination
import com.autoverse.launcher.core.navigation.supportsOpenApp

/**
 * Navigation handoff surface. The destination is validated as it is typed
 * (live parse via [parseNavigationDestination]) and the Navigate action is
 * only enabled for a valid query or coordinate pair. Providers without a
 * documented destination format (OpenOnly) never show a field — they offer a
 * single explicit Open action instead, so unsupported handoffs are explained
 * rather than silently attempted. When [dense] is true the decorative hero
 * marker and input hint are dropped so the card fits short split layouts.
 */
@Composable
fun NavigationCard(
    resolved: ResolvedNavigationProvider,
    modifier: Modifier = Modifier,
    onHandoff: (NavigationDestination) -> Unit = {},
    onOpenApp: () -> Unit = {},
    onChangeProvider: () -> Unit = {},
    dense: Boolean = false,
) {
    WidgetContainer(title = "NAVIGATION", icon = Icons.Outlined.Navigation, modifier = modifier) {
        NavigationContent(
            resolved = resolved,
            dense = dense,
            onHandoff = onHandoff,
            onOpenApp = onOpenApp,
            onChangeProvider = onChangeProvider,
        )
    }
}

@Composable
private fun ColumnScope.NavigationContent(
    resolved: ResolvedNavigationProvider,
    dense: Boolean,
    onHandoff: (NavigationDestination) -> Unit,
    onOpenApp: () -> Unit,
    onChangeProvider: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val provider = resolved.provider
    var destinationText by remember { mutableStateOf("") }
    val parseResult = remember(destinationText) { parseNavigationDestination(destinationText) }

    Spacer(Modifier.weight(1f))
    if (provider.supportsDestination) {
        if (!dense) {
            HeroMarker()
            Spacer(Modifier.size(AutoVerseSpacing.lg))
        }
        DestinationField(
            value = destinationText,
            onValueChange = { destinationText = it },
            isError = parseResult is DestinationParseResult.Invalid,
        )
        (parseResult as? DestinationParseResult.Invalid)?.let { invalid ->
            Spacer(Modifier.size(AutoVerseSpacing.xxs))
            Text(
                text = destinationFailureMessage(invalid.reason),
                style = MaterialTheme.typography.labelMedium,
                color = colors.danger,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (parseResult is DestinationParseResult.Empty && !dense) {
            Spacer(Modifier.size(AutoVerseSpacing.xs))
            Text(
                text = "Address, place name, or \u201clat, lng\u201d",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.size(AutoVerseSpacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = AutoVerseSpacing.sm,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            PrimaryAction(
                text = "Navigate",
                onClick = {
                    (parseResult as? DestinationParseResult.Valid)
                        ?.let { onHandoff(it.destination) }
                },
                enabled = parseResult is DestinationParseResult.Valid,
            )
            if (provider.supportsOpenApp) {
                TransportButton(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Open ${provider.label}",
                    onClick = onOpenApp,
                )
            }
        }
    } else {
        Text(
            text = "No documented destination format for this app. AutoVerse can only open it.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(AutoVerseSpacing.lg))
        PrimaryAction(
            text = "Open ${provider.label}",
            onClick = onOpenApp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
    Spacer(Modifier.weight(1f))
    ProviderMetaRow(resolved = resolved, onChangeProvider = onChangeProvider)
}

/**
 * Single-row navigation control for the minimal layout: destination field,
 * primary Navigate control, and (when the target supports it) an open-app
 * control. Open-only providers replace the field with one explicit Open
 * action. Shares the same live validation as [NavigationCard].
 */
@Composable
fun CompactNavigationRow(
    resolved: ResolvedNavigationProvider,
    modifier: Modifier = Modifier,
    onHandoff: (NavigationDestination) -> Unit = {},
    onOpenApp: () -> Unit = {},
) {
    val colors = LocalAutoVerseColors.current
    val provider = resolved.provider
    var destinationText by remember { mutableStateOf("") }
    val parseResult = remember(destinationText) { parseNavigationDestination(destinationText) }

    CockpitCard(modifier = modifier) {
        if (provider.supportsDestination) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
            ) {
                DestinationField(
                    value = destinationText,
                    onValueChange = { destinationText = it },
                    isError = parseResult is DestinationParseResult.Invalid,
                    modifier = Modifier.weight(1f),
                )
                TransportButton(
                    icon = Icons.Outlined.Navigation,
                    contentDescription = "Navigate",
                    onClick = {
                        (parseResult as? DestinationParseResult.Valid)
                            ?.let { onHandoff(it.destination) }
                    },
                    variant = TransportVariant.PRIMARY,
                    enabled = parseResult is DestinationParseResult.Valid,
                )
                if (provider.supportsOpenApp) {
                    TransportButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Open ${provider.label}",
                        onClick = onOpenApp,
                    )
                }
            }
            (parseResult as? DestinationParseResult.Invalid)?.let { invalid ->
                Spacer(Modifier.size(AutoVerseSpacing.xs))
                Text(
                    text = destinationFailureMessage(invalid.reason),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.danger,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            PrimaryAction(
                text = "Open ${provider.label}",
                onClick = onOpenApp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ColumnScope.HeroMarker() {
    val colors = LocalAutoVerseColors.current
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(AutoVerseSpacing.heroChip)
            .clip(CircleShape)
            .background(colors.surfaceHighlight),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Outlined.Navigation,
            contentDescription = null,
            modifier = Modifier.size(AutoVerseIcons.hero),
            tint = colors.accent,
        )
    }
}

/**
 * Destination field shared by the home card and, since Phase 18, the
 * navigation page's search card. Renders through [AutoVerseTextField] so
 * both surfaces keep one styling source; hosts can slot a trailing icon
 * (for example a clear-search action).
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

@Composable
private fun ProviderMetaRow(
    resolved: ResolvedNavigationProvider,
    onChangeProvider: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm),
    ) {
        Text(
            text = if (resolved.preferredMissing) {
                "Preferred app isn\u2019t installed \u2014 using system default"
            } else {
                "Preferred \u00b7 ${resolved.provider.label}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (resolved.preferredMissing) colors.warning else colors.textFaint,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        TextAction(text = "Change", onClick = onChangeProvider)
    }
}
