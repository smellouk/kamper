package com.smellouk.kamper.ui.compose

// Apple (iOS / macOS / tvOS): Skia's embedded font lacks emoji-only glyphs and
// can't tint colour-emoji characters. Geometric Shapes block (U+25A0–U+25FF)
// are present as monochrome outlines in every system text font.
internal actual object ChipIcons {
    actual val cpu = "●"
    actual val gpu = "▣"
    actual val fps = "▶"
    actual val mem = "■"
    actual val net = "▼"
    actual val jank = "◆"
    actual val gc = "○"
    actual val thermal = "▲"
    actual val issues = "◆"
    actual val dark = "◑"
    actual val light = "○"
}
