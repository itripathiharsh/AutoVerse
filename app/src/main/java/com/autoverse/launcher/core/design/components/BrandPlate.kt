package com.autoverse.launcher.core.design.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoverse.launcher.R
import com.autoverse.launcher.core.design.LocalAutoVerseColors
import com.autoverse.launcher.core.design.tokens.AutoVerseSpacing

/** The logo mark's own aspect ratio (transparent PNG, 486×259). */
const val BRAND_MARK_ASPECT = 486f / 259f

private val BrandBlue = Color(0xFF2F86F6)

/**
 * Brand identity: the AutoVerse infinity mark (transparent, no backdrop)
 * plus the "AutoVerse" name. "Auto" is brand blue; "Verse" takes the theme's
 * primary text colour so the name reads on light and dark themes alike —
 * no black plate behind it.
 */
@Composable
fun BrandPlate(
    modifier: Modifier = Modifier,
    logoHeight: Dp = 40.dp,
) {
    val colors = LocalAutoVerseColors.current
    val textSize = (logoHeight.value * 0.6f).sp
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(height = logoHeight)
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
            color = colors.textPrimary,
        )
    }
}

/** The transparent infinity mark alone, sized by height. */
@Composable
fun BrandMark(height: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.autoverse_brand_mark),
        contentDescription = "AutoVerse",
        modifier = modifier
            .height(height)
            .width(height * BRAND_MARK_ASPECT),
    )
}
