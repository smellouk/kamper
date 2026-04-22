package com.smellouk.kamper.macos.ui

import platform.AppKit.*

object Theme {
    val BASE     = nsColor(0x1E1E2E)
    val MANTLE   = nsColor(0x181825)
    val SURFACE0 = nsColor(0x313244)
    val SURFACE1 = nsColor(0x45475A)
    val OVERLAY  = nsColor(0x585B70)
    val TEXT     = nsColor(0xCDD6F4)
    val SUBTEXT  = nsColor(0xA6ADC8)
    val MUTED    = nsColor(0x6C7086)

    val BLUE   = nsColor(0x89B4FA)
    val GREEN  = nsColor(0xA6E3A1)
    val YELLOW = nsColor(0xF9E2AF)
    val PEACH  = nsColor(0xFAB387)
    val MAUVE  = nsColor(0xCBA6F7)
    val TEAL   = nsColor(0x94E2D5)
    val RED    = nsColor(0xF38BA8)

    val HEADER_FONT = NSFont.boldSystemFontOfSize(15.0)
    val LABEL_FONT  = NSFont.systemFontOfSize(13.0)
    val MONO_FONT   = NSFont.monospacedSystemFontOfSize(13.0, NSFontWeightRegular)
    val MONO_SMALL  = NSFont.monospacedSystemFontOfSize(11.0, NSFontWeightRegular)
    val HINT_FONT   = NSFont.systemFontOfSize(11.0)

    private fun nsColor(rgb: Int): NSColor {
        val r = ((rgb shr 16) and 0xFF) / 255.0
        val g = ((rgb shr 8) and 0xFF) / 255.0
        val b = (rgb and 0xFF) / 255.0
        return NSColor.colorWithCalibratedRed(r, green = g, blue = b, alpha = 1.0)
    }
}
