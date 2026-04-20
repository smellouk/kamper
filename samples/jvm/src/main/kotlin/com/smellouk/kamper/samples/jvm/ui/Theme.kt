package com.smellouk.kamper.samples.jvm.ui

import java.awt.Color
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.UIManager

object Theme {
    // Catppuccin Mocha palette
    val BASE        = Color(0x1E1E2E)
    val MANTLE      = Color(0x181825)
    val SURFACE0    = Color(0x313244)
    val SURFACE1    = Color(0x45475A)
    val OVERLAY     = Color(0x585B70)
    val TEXT        = Color(0xCDD6F4)
    val SUBTEXT     = Color(0xA6ADC8)
    val MUTED       = Color(0x6C7086)

    val BLUE        = Color(0x89B4FA)
    val GREEN       = Color(0xA6E3A1)
    val YELLOW      = Color(0xF9E2AF)
    val PEACH       = Color(0xFAB387)
    val MAUVE       = Color(0xCBA6F7)
    val TEAL        = Color(0x94E2D5)
    val RED         = Color(0xF38BA8)
    val SKY         = Color(0x89DCEB)

    val HEADER_FONT = Font("SansSerif", Font.BOLD, 15)
    val LABEL_FONT  = Font("SansSerif", Font.PLAIN, 13)
    val MONO_FONT   = Font("Monospaced", Font.BOLD, 13)
    val HINT_FONT   = Font("SansSerif", Font.ITALIC, 11)

    fun applyGlobals() {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName())
        UIManager.put("TabbedPane.background",        BASE)
        UIManager.put("TabbedPane.foreground",        TEXT)
        UIManager.put("TabbedPane.selected",          SURFACE0)
        UIManager.put("TabbedPane.contentAreaColor",  BASE)
        UIManager.put("TabbedPane.tabAreaBackground", MANTLE)
        UIManager.put("TabbedPane.borderColor",       SURFACE1)
        UIManager.put("TabbedPane.font",              Font("SansSerif", Font.PLAIN, 13))
        UIManager.put("Panel.background",             BASE)
    }

    fun JButton.applyStyle(accent: Color = SURFACE1): JButton = apply {
        isOpaque     = true
        isBorderPainted = true
        isFocusPainted  = false
        background   = accent
        foreground   = TEXT
        font         = Font("SansSerif", Font.PLAIN, 12)
        border       = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(OVERLAY, 1),
            BorderFactory.createEmptyBorder(5, 14, 5, 14)
        )
    }
}
