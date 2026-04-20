package com.smellouk.kamper.samples.jvm.ui

import com.smellouk.kamper.fps.FpsInfo
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.cos
import kotlin.math.sin

class FpsPanel : JPanel(BorderLayout(0, 0)) {

    private val fpsLabel = JLabel("--", SwingConstants.CENTER).apply {
        foreground = Theme.BLUE
        font = Font("Monospaced", Font.BOLD, 72)
        background = Theme.BASE
        isOpaque = true
    }
    private val unitLabel = JLabel("fps", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("SansSerif", Font.PLAIN, 16)
    }
    private val canvas = AnimationCanvas()
    private val hintLabel = JLabel("60 Hz timer-based measurement", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Theme.HINT_FONT
    }

    init {
        background = Theme.BASE

        val topCard = JPanel(BorderLayout(0, 2)).apply {
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(24, 20, 16, 20)
            add(fpsLabel, BorderLayout.CENTER)
            add(unitLabel, BorderLayout.SOUTH)
        }

        val footer = JPanel(BorderLayout()).apply {
            background = Theme.BASE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SURFACE0),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)
            )
            add(hintLabel, BorderLayout.CENTER)
        }

        add(topCard, BorderLayout.NORTH)
        add(canvas, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    fun update(info: FpsInfo) {
        if (info == FpsInfo.INVALID) return
        SwingUtilities.invokeLater { fpsLabel.text = info.fps.toString() }
    }
}

private class AnimationCanvas : JPanel() {
    private var angle = 0.0
    private val timer = Timer(16) { angle += 0.025; repaint() }

    init {
        background = Theme.MANTLE
        preferredSize = Dimension(0, 220)
        timer.start()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val cx = width / 2
        val cy = height / 2
        val r1 = minOf(width, height) * 0.30
        val r2 = minOf(width, height) * 0.15

        val palette = arrayOf(Theme.BLUE, Theme.GREEN, Theme.YELLOW, Theme.PEACH, Theme.MAUVE, Theme.TEAL)

        // Outer ring
        for (i in palette.indices) {
            val a = angle + i * Math.PI * 2 / palette.size
            val x = (cx + r1 * cos(a)).toInt()
            val y = (cy + r1 * sin(a)).toInt()
            g2.color = palette[i]
            g2.fillOval(x - 14, y - 14, 28, 28)
        }
        // Inner ring (counter-rotate)
        for (i in palette.indices) {
            val a = -angle * 1.5 + i * Math.PI * 2 / palette.size
            val x = (cx + r2 * cos(a)).toInt()
            val y = (cy + r2 * sin(a)).toInt()
            g2.color = palette[(i + 3) % palette.size].let {
                Color(it.red, it.green, it.blue, 180)
            }
            g2.fillOval(x - 7, y - 7, 14, 14)
        }
        // Centre dot
        g2.color = Theme.SURFACE1
        g2.fillOval(cx - 5, cy - 5, 10, 10)
    }
}
