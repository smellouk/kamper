package com.smellouk.konitor.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class KonitorColors(
    val BASE: Color,
    val SURFACE: Color,
    val SURFACE1: Color,
    val OVERLAY: Color,
    val BORDER: Color,
    val BLUE: Color,
    val GREEN: Color,
    val PEACH: Color,
    val TEAL: Color,
    val RED: Color,
    val WARNING: Color,
    val YELLOW: Color,
    val MAUVE: Color,
    val TEXT: Color,
    val SUBTEXT: Color,
    val SCRIM: Color
) {
    companion object {
        fun dark() = KonitorColors(
            BASE     = Color(0xFF1E1E2E),
            SURFACE  = Color(0xFF313244),
            SURFACE1 = Color(0xFF45475A),
            OVERLAY  = Color(0xE61E1E2E),
            BORDER   = Color(0x33FFFFFF),
            BLUE     = Color(0xFF89B4FA),
            GREEN    = Color(0xFFA6E3A1),
            PEACH    = Color(0xFFFAB387),
            TEAL     = Color(0xFF94E2D5),
            RED      = Color(0xFFF38BA8),
            WARNING  = Color(0xFFFFA500),
            YELLOW   = Color(0xFFF9E2AF),
            MAUVE    = Color(0xFFCBA6F7),
            TEXT     = Color(0xFFCDD6F4),
            SUBTEXT  = Color(0xFFA6ADC8),
            SCRIM    = Color(0x991E1E2E)
        )
        fun light() = KonitorColors(
            BASE     = Color(0xFFEFF1F5),
            SURFACE  = Color(0xFFE6E9EF),
            SURFACE1 = Color(0xFFDCE0E8),
            OVERLAY  = Color(0xE6EFF1F5),
            BORDER   = Color(0x33000000),
            BLUE     = Color(0xFF1E66F5),
            GREEN    = Color(0xFF40A02B),
            PEACH    = Color(0xFFFE640B),
            TEAL     = Color(0xFF179299),
            RED      = Color(0xFFD20F39),
            WARNING  = Color(0xFFFFA500),
            YELLOW   = Color(0xFFDF8E1D),
            MAUVE    = Color(0xFF8839EF),
            TEXT     = Color(0xFF4C4F69),
            SUBTEXT  = Color(0xFF5C5F77),
            SCRIM    = Color(0x991E1E2E)
        )
    }
}

internal val LocalKonitorColors = compositionLocalOf { KonitorColors.dark() }

@Composable
internal fun KonitorThemeProvider(isDark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKonitorColors provides if (isDark) KonitorColors.dark() else KonitorColors.light(),
        content = content
    )
}

internal object KonitorTheme {
    val BASE: Color     @Composable get() = LocalKonitorColors.current.BASE
    val SURFACE: Color  @Composable get() = LocalKonitorColors.current.SURFACE
    val SURFACE1: Color @Composable get() = LocalKonitorColors.current.SURFACE1
    val OVERLAY: Color  @Composable get() = LocalKonitorColors.current.OVERLAY
    val BORDER: Color   @Composable get() = LocalKonitorColors.current.BORDER
    val BLUE: Color     @Composable get() = LocalKonitorColors.current.BLUE
    val GREEN: Color    @Composable get() = LocalKonitorColors.current.GREEN
    val PEACH: Color    @Composable get() = LocalKonitorColors.current.PEACH
    val TEAL: Color     @Composable get() = LocalKonitorColors.current.TEAL
    val RED: Color      @Composable get() = LocalKonitorColors.current.RED
    val WARNING: Color  @Composable get() = LocalKonitorColors.current.WARNING
    val YELLOW: Color   @Composable get() = LocalKonitorColors.current.YELLOW
    val MAUVE: Color    @Composable get() = LocalKonitorColors.current.MAUVE
    val TEXT: Color     @Composable get() = LocalKonitorColors.current.TEXT
    val SUBTEXT: Color  @Composable get() = LocalKonitorColors.current.SUBTEXT
    val SCRIM: Color    @Composable get() = LocalKonitorColors.current.SCRIM
}
