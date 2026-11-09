package com.autoverse.launcher.core.design.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * One corner family: panels 20dp, inner elements step down by their padding
 * (16 / 12 / 10) so nested corners stay concentric. Pills are for chips and
 * primary buttons only.
 */
object AutoVerseShapes {
    val card = RoundedCornerShape(20.dp)
    val tile = RoundedCornerShape(16.dp)
    val row = RoundedCornerShape(12.dp)
    val small = RoundedCornerShape(10.dp)
    val pill = RoundedCornerShape(50)
}

/**
 * Hierarchy comes from clearly stepped surface tones, not outlines: panels
 * are borderless. Borders remain only for focus/pressed/selected states and
 * input fields, using the translucent outline tokens.
 */
object AutoVerseBorders {
    val hairline = 1.dp
    val emphasis = 2.dp
}
