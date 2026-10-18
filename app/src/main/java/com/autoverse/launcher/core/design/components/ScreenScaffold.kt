package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing
import com.autoverse.launcher.core.performance.LocalPerformanceSpec

/** Back affordance + screen title, aligned with the dashboard's top strip. */
@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAutoVerseColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.xs),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colors.textSecondary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )
    }
}

/**
 * Gradient background + safe insets + standard screen padding + fixed header,
 * with a token-spaced content column. Content scrolls by default (Settings/
 * Diagnostics); pass [scrollable] = false when the content owns its own
 * scrolling (e.g. a LazyVerticalGrid in the app drawer), which also lets
 * content use `Modifier.weight` and `fillMaxSize`.
 */
@Composable
fun AutoVerseScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    val spec = LocalPerformanceSpec.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (spec.flatSurfaces) {
                    Modifier.background(colors.backgroundTop)
                } else {
                    Modifier.background(
                        Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)),
                    )
                },
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                horizontal = AutoVerseSpacing.screenHorizontal,
                vertical = AutoVerseSpacing.screenVertical,
            ),
        verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
    ) {
        ScreenHeader(title = title, onBack = onBack)
        if (scrollable) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.stackGap),
            ) {
                content()
            }
        } else {
            content()
        }
    }
}
