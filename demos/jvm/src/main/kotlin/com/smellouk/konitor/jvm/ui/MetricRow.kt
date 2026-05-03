package com.smellouk.konitor.jvm.ui

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JPanel

class MetricRow(
    private val label: String,
    private val barColor: Color,
    private val trackColor: Color = Color(0x2A2A3E)
) : JPanel() {

    var fraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            repaint()
        }

    var valueText: String = "0.0%"
        set(value) {
            field = value
            repaint()
        }

    init {
        isOpaque = false
        preferredSize = Dimension(0, ROW_HEIGHT)
        minimumSize = Dimension(0, ROW_HEIGHT)
        maximumSize = Dimension(Int.MAX_VALUE, ROW_HEIGHT)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        val labelW = LABEL_WIDTH
        val valueW = VALUE_WIDTH
        val barX = labelW
        val barW = width - labelW - valueW - BAR_H_PAD * 2
        val barY = (height - BAR_THICKNESS) / 2
        val arc = BAR_THICKNESS

        // Label
        g2.font = LABEL_FONT
        g2.color = LABEL_COLOR
        g2.drawString(label, 0, height / 2 + g2.fontMetrics.ascent / 2 - 1)

        // Track
        g2.color = trackColor
        g2.fillRoundRect(barX + BAR_H_PAD, barY, barW, BAR_THICKNESS, arc, arc)

        // Fill
        val fillW = (barW * fraction).toInt().coerceAtLeast(if (fraction > 0f) arc else 0)
        if (fillW > 0) {
            val grad = GradientPaint(
                barX.toFloat(), 0f, barColor.brighter(),
                (barX + fillW).toFloat(), 0f, barColor
            )
            g2.paint = grad
            g2.fillRoundRect(barX + BAR_H_PAD, barY, fillW, BAR_THICKNESS, arc, arc)

            // Subtle glow edge
            g2.paint = barColor.withAlpha(60)
            g2.stroke = BasicStroke(1f)
            g2.drawRoundRect(barX + BAR_H_PAD, barY, fillW, BAR_THICKNESS, arc, arc)
        }

        // Value
        g2.paint = VALUE_COLOR
        g2.font = VALUE_FONT
        val valStr = valueText
        val valX = width - g2.fontMetrics.stringWidth(valStr)
        g2.drawString(valStr, valX, height / 2 + g2.fontMetrics.ascent / 2 - 1)
    }

    private fun Color.withAlpha(a: Int) = Color(red, green, blue, a)

    companion object {
        const val ROW_HEIGHT = 38
        private const val LABEL_WIDTH = 72
        private const val VALUE_WIDTH = 58
        private const val BAR_H_PAD = 8
        private const val BAR_THICKNESS = 14
        private val LABEL_FONT = Font("SansSerif", Font.PLAIN, 13)
        private val VALUE_FONT = Font("Monospaced", Font.BOLD, 13)
        private val LABEL_COLOR = Color(0xCDD6F4.or(0xFF shl 24), true)
        private val VALUE_COLOR = Color(0xA6ADC8.or(0xFF shl 24), true)
    }
}
