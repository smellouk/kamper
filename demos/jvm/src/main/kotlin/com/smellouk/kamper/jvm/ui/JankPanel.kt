package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class JankPanel : JPanel(BorderLayout(0, 0)) {

    private val bigLabel = JLabel("—", SwingConstants.CENTER).apply {
        foreground = Theme.MAUVE
        font = Font("Monospaced", Font.BOLD, 72)
        background = Theme.BASE
        isOpaque = true
    }
    private val unitLabel = JLabel("dropped frames / window", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("SansSerif", Font.PLAIN, 14)
    }
    private val ratioLabel = JLabel("Janky ratio:  —", SwingConstants.LEFT).apply {
        foreground = Theme.TEXT
        font = Theme.LABEL_FONT
    }
    private val worstLabel = JLabel("Worst frame:  —", SwingConstants.LEFT).apply {
        foreground = Theme.TEXT
        font = Theme.LABEL_FONT
    }
    private val simulateButton = JButton("Simulate Jank").applyStyle(Theme.SURFACE0)

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
            add(ratioLabel)
            add(worstLabel)
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
            Kamper.logEvent("jank_simulate")
            SwingUtilities.invokeLater {
                val end = System.currentTimeMillis() + 200
                while (System.currentTimeMillis() < end) {}
            }
        }

        add(topCard, BorderLayout.NORTH)
        add(statsPanel, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    fun update(info: JankInfo) {
        if (info == JankInfo.INVALID) return
        SwingUtilities.invokeLater {
            if (info == JankInfo.UNSUPPORTED) {
                bigLabel.text   = "N/A"
                bigLabel.foreground = Theme.MUTED
                unitLabel.text  = "not supported on JVM"
                ratioLabel.text = "Janky ratio:  —"
                worstLabel.text = "Worst frame:  —"
                simulateButton.isEnabled = false
                return@invokeLater
            }
            bigLabel.text  = info.droppedFrames.toString()
            bigLabel.foreground = Theme.MAUVE
            unitLabel.text = "dropped frames / window"
            ratioLabel.text = "Janky ratio:  ${"%.1f".format(info.jankyFrameRatio * 100f)}%"
            worstLabel.text = "Worst frame:  ${info.worstFrameMs} ms"
            simulateButton.isEnabled = true
        }
    }
}
