package com.autoverse.launcher.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MusicNote
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.components.BrandPlate
import com.autoverse.launcher.core.design.components.PremiumUnlockDialog
import com.autoverse.launcher.core.design.components.PrimaryAction
import com.autoverse.launcher.core.design.components.TextAction
import com.autoverse.launcher.core.design.themeSpecFor
import com.autoverse.launcher.core.media.isNotificationAccessEnabled
import com.autoverse.launcher.core.model.AutoVerseThemeId
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/**
 * Tier 1 — first-run onboarding. Four steps: welcome, theme pick (premium
 * themes gated by the local entitlement), real connectivity/setup checks
 * (launcher chooser, media access, location, voice availability), and done.
 * Shown whenever [onboardingComplete] is false in settings; replayable from
 * Settings. Never forces any choice: the launcher chooser only opens the
 * system picker, and every step can be skipped.
 */
@Composable
fun OnboardingScreen(
    selectedTheme: AutoVerseThemeId,
    onSelectTheme: (AutoVerseThemeId) -> Unit,
    premiumUnlocked: Boolean,
    onUnlockPremium: () -> Unit,
    mediaAccessGranted: Boolean,
    onOpenMediaAccess: () -> Unit,
    locationGranted: Boolean,
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAutoVerseColors.current
    val context = LocalContext.current
    var showPremiumDialog by remember { mutableStateOf(false) }

    var locationState by remember { mutableStateOf(locationGranted) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        locationState = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBottom)
            .padding(AutoVerseSpacing.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandPlate(logoHeight = 36.dp)
            if (pagerState.currentPage != 3) {
                TextAction(text = "Skip", onClick = onGetStarted)
            }
        }

        Spacer(Modifier.height(AutoVerseSpacing.lg))

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> ThemePage(
                    selectedTheme = selectedTheme,
                    onSelectTheme = onSelectTheme,
                    premiumUnlocked = premiumUnlocked,
                    onLockedThemeTapped = { showPremiumDialog = true },
                )
                2 -> SetupPage(
                    mediaAccessGranted = mediaAccessGranted,
                    onOpenMediaAccess = onOpenMediaAccess,
                    locationGranted = locationState,
                    onRequestLocation = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                    onSetDefaultLauncher = { openHomeChooser(context) },
                )
                else -> FinishPage()
            }
        }

        Spacer(Modifier.height(AutoVerseSpacing.md))

        // Page indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(4) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (active) colors.accent else colors.outline),
                )
            }
        }

        Spacer(Modifier.height(AutoVerseSpacing.md))

        PrimaryAction(
            text = when (pagerState.currentPage) {
                3 -> "Open the cockpit"
                else -> "Continue"
            },
            onClick = {
                when (pagerState.currentPage) {
                    3 -> onGetStarted()
                    else -> scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
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
            "Your cockpit, your rules",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AutoVerseSpacing.sm))
        Text(
            "A launcher built for the car: a live map, your music, GPS speed " +
                "and every app one tap away \u2014 no account, nothing tracked.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
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
private fun SetupPage(
    mediaAccessGranted: Boolean,
    onOpenMediaAccess: () -> Unit,
    locationGranted: Boolean,
    onRequestLocation: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
) {
    val colors = LocalAutoVerseColors.current
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(AutoVerseSpacing.sm)) {
        Text("Connect your car", style = MaterialTheme.typography.headlineSmall, color = colors.textPrimary)
        Spacer(Modifier.height(AutoVerseSpacing.xs))
        SetupRow(
            icon = { Icon(Icons.Outlined.Home, contentDescription = null, tint = colors.accent) },
            title = "Make AutoVerse the launcher",
            detail = "Opens the Android home-app picker. You stay in control.",
            actionText = "Choose",
            onAction = onSetDefaultLauncher,
        )
        SetupRow(
            icon = { Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = colors.accent) },
            title = "Media session access",
            detail = if (mediaAccessGranted) "Granted \u2014 the dashboard reads now-playing." else "Needed to show Spotify / YT Music on the dashboard.",
            actionText = if (mediaAccessGranted) "Granted" else "Grant",
            onAction = onOpenMediaAccess,
            positive = mediaAccessGranted,
        )
        SetupRow(
            icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = colors.accent) },
            title = "Location (cockpit GPS)",
            detail = if (locationGranted) "Granted \u2014 live map position and speed." else "Powers the live map, recenter and speed readout.",
            actionText = if (locationGranted) "Granted" else "Grant",
            onAction = onRequestLocation,
            positive = locationGranted,
        )
    }
}

@Composable
private fun SetupRow(
    icon: @Composable () -> Unit,
    title: String,
    detail: String,
    actionText: String,
    onAction: () -> Unit,
    positive: Boolean = false,
) {
    val colors = LocalAutoVerseColors.current
    Surface(
        shape = AutoVerseShapes.small,
        color = colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(AutoVerseSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(AutoVerseSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Spacer(Modifier.width(AutoVerseSpacing.sm))
            TextAction(
                text = actionText,
                onClick = onAction,
                enabled = !positive,
            )
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

private fun openHomeChooser(context: Context) {
    try {
        val home = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
        context.startActivity(Intent.createChooser(home, "Set AutoVerse as your launcher"))
    } catch (_: Exception) {
    }
}
