package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.gpu.GpuInfo
import java.awt.AlphaComposite
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.util.Random
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer

class GpuPanel : JPanel(BorderLayout(0, 0)) {

    private companion object {
        const val UNSUPPORTED_LABEL = "Unsupported"
        const val MEMORY_NA = "N/A"
        const val UNKNOWN_UTIL = "—%"
        const val NA = "N/A"
        const val SHAPES_PER_FRAME = 300
        const val REPAINT_INTERVAL_MS = 16
        const val MAX_RADIUS = 60
        const val MIN_RADIUS = 8
        const val COLOR_BOUND = 256
        const val CIRCLE_ALPHA = 0.7f
    }

    private val rng = Random()
    private var stressTimer: Timer? = null

    private val stressCanvas = object : JPanel() {
        init {
            background = Color(0x0D, 0x0D, 0x1A)
            isOpaque = true
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, CIRCLE_ALPHA)
            repeat(SHAPES_PER_FRAME) {
                g2.color = Color(rng.nextInt(COLOR_BOUND), rng.nextInt(COLOR_BOUND), rng.nextInt(COLOR_BOUND))
                val r = rng.nextInt(MAX_RADIUS) + MIN_RADIUS
                val x = rng.nextInt(width.coerceAtLeast(1))
                val y = rng.nextInt(height.coerceAtLeast(1))
                g2.fillOval(x - r, y - r, r * 2, r * 2)
            }
        }
    }

    private val stressButton = JButton("STRESS GPU").apply {
        background = Theme.SURFACE0
        foreground = Theme.TEXT
        font = Theme.LABEL_FONT
        isFocusPainted = false
        addActionListener { if (stressTimer == null) startStress() else stopStress() }
    }

    private val bigLabel = JLabel("—", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("Monospaced", Font.BOLD, 48)
        background = Theme.MANTLE
        isOpaque = true
    }
    private val unitLabel = JLabel("GPU usage %", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("SansSerif", Font.PLAIN, 14)
    }
    private val memoryRow      = statRow("Memory",        NA)
    private val appUtilRow     = statRow("App Util",      NA)
    private val rendererRow    = statRow("Renderer Util", NA)
    private val tilerRow       = statRow("Tiler Util",    NA)
    private val computeRow     = statRow("Compute Util",  NA)

    init {
        background = Theme.BASE

        // ── Header (mantle bg, big number + unit) ──────────────────────────
        val header = JPanel(BorderLayout(0, 4)).apply {
            background = Theme.MANTLE
            border = BorderFactory.createEmptyBorder(24, 20, 20, 20)
            add(bigLabel,  BorderLayout.CENTER)
            add(unitLabel, BorderLayout.SOUTH)
        }

        // ── Left column: stat rows ─────────────────────────────────────────
        val leftCol = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(16, 20, 16, 20)
            listOf(memoryRow, appUtilRow, rendererRow, tilerRow, computeRow).forEach { row ->
                add(row)
                add(divider())
            }
        }

        // ── Vertical separator ─────────────────────────────────────────────
        val sep = JPanel().apply {
            background = Theme.SURFACE0
            preferredSize = java.awt.Dimension(1, 0)
            minimumSize   = java.awt.Dimension(1, 0)
        }

        // ── Right column: stress canvas + button ───────────────────────────
        val rightCol = JPanel(BorderLayout(0, 12)).apply {
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
            add(stressCanvas,  BorderLayout.CENTER)
            add(stressButton,  BorderLayout.SOUTH)
        }

        // ── Two-column body ────────────────────────────────────────────────
        val body = JPanel(BorderLayout(0, 0)).apply {
            background = Theme.BASE
            add(leftCol,  BorderLayout.WEST)
            add(sep,      BorderLayout.CENTER)
            add(rightCol, BorderLayout.EAST)
        }
        // Equal halves: each column takes ~50 % via preferred width trick
        leftCol.preferredSize  = java.awt.Dimension(0, 0)  // will expand
        rightCol.preferredSize = java.awt.Dimension(0, 0)

        // Use GridLayout(1,2) wrapper so columns share width equally
        val twoCol = JPanel(java.awt.GridLayout(1, 2, 1, 0)).apply {
            background = Theme.SURFACE0  // 1px gap shows as divider
            add(leftCol)
            add(rightCol)
        }

        add(header, BorderLayout.NORTH)
        add(twoCol, BorderLayout.CENTER)
    }

    fun update(info: GpuInfo) {
        if (info == GpuInfo.INVALID) return
        if (info == GpuInfo.UNSUPPORTED) {
            SwingUtilities.invokeLater {
                bigLabel.text      = UNSUPPORTED_LABEL
                bigLabel.foreground = Theme.MUTED
                setStatValue(memoryRow,   MEMORY_NA)
                setStatValue(appUtilRow,  NA)
                setStatValue(rendererRow, NA)
                setStatValue(tilerRow,    NA)
                setStatValue(computeRow,  NA)
            }
            return
        }
        SwingUtilities.invokeLater {
            bigLabel.text = if (info.utilization >= 0.0) "%.1f%%".format(info.utilization) else UNKNOWN_UTIL
            bigLabel.foreground = Theme.MAUVE
            setStatValue(memoryRow,   buildMemoryText(info.usedMemoryMb, info.totalMemoryMb))
            setStatValue(appUtilRow,  formatUtil(info.appUtilization))
            setStatValue(rendererRow, formatUtil(info.rendererUtilization))
            setStatValue(tilerRow,    formatUtil(info.tilerUtilization))
            setStatValue(computeRow,  formatUtil(info.computeUtilization))
        }
    }

    private fun startStress() {
        stressButton.text = "STOP STRESS"
        stressTimer = Timer(REPAINT_INTERVAL_MS) { stressCanvas.repaint() }.also { it.start() }
    }

    private fun stopStress() {
        stressTimer?.stop()
        stressTimer = null
        stressButton.text = "STRESS GPU"
    }

    private fun buildMemoryText(usedMb: Double, totalMb: Double): String = when {
        usedMb >= 0.0 && totalMb >= 0.0 -> "%.0f MB / %.0f MB".format(usedMb, totalMb)
        totalMb >= 0.0                   -> "— / %.0f MB".format(totalMb)
        else                             -> MEMORY_NA
    }

    private fun formatUtil(value: Double): String =
        if (value >= 0.0) "%.1f%%".format(value) else NA

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun statRow(label: String, initialValue: String): JPanel =
        JPanel(BorderLayout()).apply {
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
            add(JLabel(label).apply { foreground = Theme.MUTED; font = Theme.LABEL_FONT }, BorderLayout.WEST)
            add(JLabel(initialValue, SwingConstants.RIGHT).apply {
                foreground = Theme.TEXT
                font = Font("Monospaced", Font.BOLD, Theme.LABEL_FONT.size)
                name = "value"
            }, BorderLayout.EAST)
        }

    private fun setStatValue(row: JPanel, value: String) {
        (row.components.firstOrNull { it is JLabel && it.name == "value" } as? JLabel)?.text = value
    }

    private fun divider(): JPanel = JPanel().apply {
        background = Theme.SURFACE0
        maximumSize = java.awt.Dimension(Int.MAX_VALUE, 1)
        preferredSize = java.awt.Dimension(0, 1)
    }
}
