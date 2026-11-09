package com.autoverse.launcher.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.BrandPlate
import com.autoverse.launcher.core.design.components.PremiumUnlockDialog
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.themeSpecFor
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.permissions.CarSetupStatus
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

private const val PAGE_WELCOME = 0
private const val PAGE_SETUP = 1
private const val PAGE_THEME = 2
private const val PAGE_FINISH = 3
private const val PAGE_COUNT = 4

/**
 * First-run onboarding: welcome, car setup (permissions), theme, done.
 *
 * The car-setup page is a gate: "Continue" stays locked until the required
 * items — precise location, GPS switched on, music control — are ready, and
 * the pager cannot be swiped past it. Optional items (home app, calls, OBD)
 * never block. Replayable from Settings.
 */
@Composable
fun OnboardingScreen(
    selectedTheme: AutoVerseThemeId,
    onSelectTheme: (AutoVerseThemeId) -> Unit,
    premiumUnlocked: Boolean,
    onUnlockPremium: () -> Unit,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    var showPremiumDialog by remember { mutableStateOf(false) }
    var setupStatus by remember { mutableStateOf<CarSetupStatus?>(null) }

    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val page = pagerState.currentPage
    val setupReady = setupStatus?.allRequiredGranted == true
    val continueEnabled = page != PAGE_SETUP || setupReady

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.backgroundTop, colors.backgroundBottom)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = AutoVerseSpacing.lg, vertical = AutoVerseSpacing.md),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandPlate(logoHeight = 32.dp)
            StepIndicator(current = page)
        }

        Spacer(Modifier.height(AutoVerseSpacing.md))

        // Swiping is off so nobody slides past the setup gate; the buttons move pages.
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.weight(1f),
        ) { index ->
            when (index) {
                PAGE_WELCOME -> WelcomePage()
                PAGE_SETUP -> CarSetupPanel(onStatusChange = { setupStatus = it })
                PAGE_THEME -> ThemePage(
                    selectedTheme = selectedTheme,
                    onSelectTheme = onSelectTheme,
                    premiumUnlocked = premiumUnlocked,
                    onLockedThemeTapped = { showPremiumDialog = true },
                )
                else -> FinishPage()
            }
        }

        Spacer(Modifier.height(AutoVerseSpacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.md),
        ) {
            if (page > PAGE_WELCOME) {
                TextAction(
                    text = "Back",
                    onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } },
                )
            }
            Text(
                text = when {
                    page == PAGE_SETUP && !setupReady ->
                        "Finish the REQUIRED items to continue"
                    else -> ""
                },
                style = MaterialTheme.typography.labelMedium,
                color = colors.warning,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            PrimaryAction(
                text = when (page) {
                    PAGE_WELCOME -> "Let's set up"
                    PAGE_FINISH -> "Open the cockpit"
                    else -> "Continue"
                },
                enabled = continueEnabled,
                onClick = {
                    if (page == PAGE_FINISH) {
                        onGetStarted()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(page + 1) }
                    }
                },
                modifier = Modifier.width(220.dp),
            )
        }
    }

    if (showPremiumDialog) {
        PremiumUnlockDialog(
            onUnlock = {
                onUnlockPremium()
                showPremiumDialog = false
            },
            onDismiss = { showPremiumDialog = false },
        )
    }
}

@Composable
private fun WelcomePage() {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandPlate(logoHeight = 56.dp)
        Spacer(Modifier.height(AutoVerseSpacing.lg))
        Text(
            "Welcome to your cockpit",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        Text(
            "Map, music, speed and car tools on one screen \u2014 made for driving. " +
                "Setup takes about a minute.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AutoVerseSpacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.lg)) {
            WelcomePoint(Icons.Outlined.LocationOn, "Live map")
            WelcomePoint(Icons.Outlined.MusicNote, "Music control")
            WelcomePoint(Icons.Outlined.Speed, "GPS speed")
            WelcomePoint(Icons.Outlined.GraphicEq, "Car tools")
        }
    }
}

@Composable
private fun WelcomePoint(icon: ImageVector, label: String) {
    val colors = LocalAutoVerseColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
    }
}

/** "Step 2 of 4 \u00b7 Car setup" \u2014 tells the driver how far along they are. */
@Composable
private fun StepIndicator(current: Int) {
    val colors = LocalAutoVerseColors.current
    val names = listOf("Welcome", "Car setup", "Theme", "Done")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Step ${current + 1} of $PAGE_COUNT \u00b7 ${names[current]}",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
        )
        Spacer(Modifier.width(AutoVerseSpacing.sm))
        repeat(PAGE_COUNT) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(width = if (index == current) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (index <= current) colors.accent else colors.outline),
            )
        }
    }
}

@Composable
private fun ThemePage(
    selectedTheme: AutoVerseThemeId,
    onSelectTheme: (AutoVerseThemeId) -> Unit,
    premiumUnlocked: Boolean,
    onLockedThemeTapped: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Pick your theme", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        Text(
            "Everything re-colours instantly \u2014 no restart. Premium themes are free in this early build.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(AutoVerseSpacing.md))
        Column(verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
            AutoVerseThemeId.entries.chunked(2).forEach { rowThemes ->
                Row(horizontalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
                    rowThemes.forEach { theme ->
                        ThemeSwatch(
                            theme = theme,
                            selected = theme == selectedTheme,
                            locked = theme.premium && !premiumUnlocked,
                            onClick = {
                                if (theme.premium && !premiumUnlocked) onLockedThemeTapped()
                                else onSelectTheme(theme)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AutoVerseThemeId,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val spec = themeSpecFor(theme)
    val dark = true // preview night palette for consistent swatch look
    val swatch = spec.colorsFor(dark)
    Surface(
        onClick = onClick,
        shape = AutoVerseShapes.small,
        color = if (selected) colors.surfaceHighlight else colors.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) colors.accent else colors.outline,
        ),
        modifier = modifier.alpha(if (locked) 0.55f else 1f),
    ) {
        Column(modifier = Modifier.padding(AutoVerseSpacing.sm)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(swatch.accent),
                )
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(swatch.surfaceRaised)
                        .border(1.dp, swatch.outline, CircleShape),
                )
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(swatch.backgroundTop),
                )
            }
            Spacer(Modifier.height(AutoVerseSpacing.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    theme.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (theme.premium) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (locked) colors.disabledContainer else colors.accent)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            if (locked) "PRO" else "\u2713",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (locked) colors.disabledContent else swatch.onAccent,
                        )
                    }
                } else if (selected) {
                    Text("\u2713", color = colors.accent, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun FinishPage() {
    val colors = LocalAutoVerseColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("You're set", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        Text(
            "Home is your cockpit: map, music and speed. Change anything later in " +
                "Settings \u2014 including running this setup again.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
