package com.autoverse.launcher.core.design.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object AutoVerseShapes {
    val card = RoundedCornerShape(24.dp)
    val tile = RoundedCornerShape(20.dp)
    val row = RoundedCornerShape(14.dp)
    val small = RoundedCornerShape(12.dp)
    val pill = RoundedCornerShape(50)
}

/**
 * AutoVerse is intentionally flat: hierarchy comes from surface tones and
 * hairline borders, never from shadows. No elevation tokens exist.
 */
object AutoVerseBorders {
    val hairline = 1.dp
    val emphasis = 2.dp
}
