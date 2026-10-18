package com.autoverse.launcher.core.design

enum class LayoutClass { COMPACT, MEDIUM, EXPANDED }

fun layoutClassFor(widthDp: Int): LayoutClass = when {
    widthDp < 600 -> LayoutClass.COMPACT
    widthDp < 840 -> LayoutClass.MEDIUM
    else -> LayoutClass.EXPANDED
}
