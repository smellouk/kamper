package com.smellouk.kamper.compose.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object KamperColors {
    val bg = Color(0xFF1E1E2E)
    val mantle = Color(0xFF181825)
    val surface0 = Color(0xFF313244)
    val surface1 = Color(0xFF45475A)
    val overlay1 = Color(0xFF7F849C)
    val subtext1 = Color(0xFFBAC2DE)
    val text = Color(0xFFCDD6F4)
    val lavender = Color(0xFFB4BEFE)
    val blue = Color(0xFF89B4FA)
    val teal = Color(0xFF94E2D5)
    val green = Color(0xFFA6E3A1)
    val yellow = Color(0xFFF9E2AF)
    val peach = Color(0xFFFAB387)
    val red = Color(0xFFF38BA8)
    val mauve = Color(0xFFCBA6F7)
}

private val KamperColorScheme = darkColorScheme(
    background = KamperColors.bg,
    surface = KamperColors.surface0,
    surfaceVariant = KamperColors.surface1,
    primary = KamperColors.blue,
    secondary = KamperColors.mauve,
    tertiary = KamperColors.teal,
    onBackground = KamperColors.text,
    onSurface = KamperColors.text,
    error = KamperColors.red
)

@Composable
fun KamperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KamperColorScheme,
        content = content
    )
}
