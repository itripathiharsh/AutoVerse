package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoverse.launcher.R
import com.autoverse.launcher.core.design.tokens.AutoVerseShapes
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/**
 * The logo's native charcoal, kept constant on purpose: the chrome half of
 * the mark is light and would wash out on day themes, so the plate
 * reproduces the brand's own dark backdrop in every theme instead of
 * tinting or recolouring the mark.
 */
private val BrandPlateColor = Color(0xFF141B22)

private val BrandBlue = Color(0xFF4D93FA)
private val BrandChrome = Color(0xFFDCE4EF)

/**
 * Brand identity: the two-tone infinity mark drawn as a crisp vector (no
 * baked-in wordmark bitmap) plus the "AutoVerse" name set in the app's own
 * typography — sharp at every density and size, legible on the charcoal
 * plate in day and night themes alike.
 */
@Composable
fun BrandPlate(
    modifier: Modifier = Modifier,
    logoHeight: Dp = 40.dp,
) {
    val textSize = (logoHeight.value * 0.6f).sp
    Row(
        modifier = modifier
            .clip(AutoVerseShapes.small)
            .background(BrandPlateColor)
            .padding(horizontal = AutoVerseSpacing.sm, vertical = AutoVerseSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.autoverse_logo_mark),
            contentDescription = "AutoVerse",
            modifier = Modifier.height(logoHeight).width(logoHeight * 40f / 24f),
        )
        Spacer(Modifier.width(AutoVerseSpacing.xs))
        Text(
            text = "Auto",
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            color = BrandBlue,
        )
        Text(
            text = "Verse",
            fontSize = textSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
            color = BrandChrome,
        )
    }
}
