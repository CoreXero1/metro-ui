package com.codeancy.metroui.common.utils

import androidx.compose.ui.graphics.Color

fun String.hexToColor(defaultColor: Color = Color.Gray): Color {
    val cleanedHex = this.removePrefix("#").trim()
    val parsedHex = cleanedHex.toLongOrNull(16) ?: return defaultColor

    return when (cleanedHex.length) {
        6 -> Color(
            red = ((parsedHex shr 16) and 0xFF) / 255f,
            green = ((parsedHex shr 8) and 0xFF) / 255f,
            blue = (parsedHex and 0xFF) / 255f,
            alpha = 1f
        )

        8 -> Color(
            alpha = ((parsedHex shr 24) and 0xFF) / 255f,
            red = ((parsedHex shr 16) and 0xFF) / 255f,
            green = ((parsedHex shr 8) and 0xFF) / 255f,
            blue = (parsedHex and 0xFF) / 255f
        )

        else -> defaultColor
    }
}