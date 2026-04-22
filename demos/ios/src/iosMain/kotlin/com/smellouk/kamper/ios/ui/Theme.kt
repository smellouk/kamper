package com.smellouk.kamper.ios.ui

import platform.UIKit.*

object Theme {
    val BASE     = uiColor(0x1E1E2E)
    val MANTLE   = uiColor(0x181825)
    val SURFACE0 = uiColor(0x313244)
    val SURFACE1 = uiColor(0x45475A)
    val TEXT     = uiColor(0xCDD6F4)
    val SUBTEXT  = uiColor(0xA6ADC8)
    val MUTED    = uiColor(0x6C7086)

    val BLUE   = uiColor(0x89B4FA)
    val GREEN  = uiColor(0xA6E3A1)
    val YELLOW = uiColor(0xF9E2AF)
    val PEACH  = uiColor(0xFAB387)
    val MAUVE  = uiColor(0xCBA6F7)
    val TEAL   = uiColor(0x94E2D5)
    val RED    = uiColor(0xF38BA8)

    val HEADER_FONT = UIFont.boldSystemFontOfSize(15.0)!!
    val LABEL_FONT  = UIFont.systemFontOfSize(13.0)!!
    val MONO_FONT   = UIFont.monospacedSystemFontOfSize(13.0, weight = 0.0)!!
    val MONO_SMALL  = UIFont.monospacedSystemFontOfSize(11.0, weight = 0.0)!!
    val HINT_FONT   = UIFont.systemFontOfSize(11.0)!!

    fun uiColor(rgb: Int): UIColor {
        val r = ((rgb shr 16) and 0xFF) / 255.0
        val g = ((rgb shr 8) and 0xFF) / 255.0
        val b = (rgb and 0xFF) / 255.0
        return UIColor(red = r, green = g, blue = b, alpha = 1.0)
    }
}
