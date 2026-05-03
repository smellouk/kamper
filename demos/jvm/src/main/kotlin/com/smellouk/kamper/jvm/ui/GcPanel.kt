package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class GcPanel : JPanel(BorderLayout(0, 0)) {

    private val bigLabel = JLabel("—", SwingConstants.CENTER).apply {
        foreground = Theme.YELLOW
        font = Font("Monospaced", Font.BOLD, 72)
        background = Theme.BASE
        isOpaque = true
    }
    private val unitLabel = JLabel("GC events / interval", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("SansSerif", Font.PLAIN, 14)
    }
    private val pauseDeltaLabel = JLabel("GC pause delta:  —", SwingConstants.LEFT).apply {
        foreground = Theme.TEXT
        font = Theme.LABEL_FONT
    }
    private val totalCountLabel = JLabel("Total GC count:  —", SwingConstants.LEFT).apply {
        foreground = Theme.TEXT
        font = Theme.LABEL_FONT
    }
    private val simulateButton = JButton("Simulate GC").applyStyle(Theme.SURFACE0)

    init {
        background = Theme.BASE

        val topCard = JPanel(BorderLayout(0, 2)).apply {
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(24, 20, 16, 20)
            add(bigLabel, BorderLayout.CENTER)
            add(unitLabel, BorderLayout.SOUTH)
        }

        val statsPanel = JPanel(GridLayout(2, 1, 0, 8)).apply {
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(16, 20, 12, 20)
            add(pauseDeltaLabel)
            add(totalCountLabel)
        }

        val footer = JPanel(BorderLayout()).apply {
            background = Theme.BASE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SURFACE0),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
            )
            add(simulateButton, BorderLayout.EAST)
        }

        simulateButton.addActionListener {
            Kamper.logEvent("gc_simulate")
            Thread {
                repeat(200_000) { ByteArray(1024) }
                System.gc()
            }.apply { isDaemon = true; start() }
        }

        add(topCard, BorderLayout.NORTH)
        add(statsPanel, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    fun update(info: GcInfo) {
        if (info == GcInfo.INVALID) return
        SwingUtilities.invokeLater {
            bigLabel.text       = info.gcCountDelta.toString()
            pauseDeltaLabel.text = "GC pause delta:  ${info.gcPauseMsDelta} ms"
            totalCountLabel.text = "Total GC count:  ${info.gcCount}"
        }
    }
}
