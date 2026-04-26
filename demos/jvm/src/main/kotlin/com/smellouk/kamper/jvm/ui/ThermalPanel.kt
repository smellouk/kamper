package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import com.smellouk.kamper.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class ThermalPanel : JPanel(BorderLayout(0, 0)) {

    private val bigLabel = JLabel("UNKNOWN", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("Monospaced", Font.BOLD, 48)
        background = Theme.BASE
        isOpaque = true
    }
    private val unitLabel = JLabel("thermal state", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("SansSerif", Font.PLAIN, 14)
    }
    private val throttlingLabel = JLabel("Throttling:  —", SwingConstants.LEFT).apply {
        foreground = Theme.TEXT
        font = Theme.LABEL_FONT
    }
    private val stressButton = JButton("Start CPU Stress").applyStyle(Theme.SURFACE0)
    private var stressExecutor: ExecutorService? = null

    init {
        background = Theme.BASE

        val topCard = JPanel(BorderLayout(0, 2)).apply {
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(24, 20, 16, 20)
            add(bigLabel, BorderLayout.CENTER)
            add(unitLabel, BorderLayout.SOUTH)
        }

        val statsPanel = JPanel(GridLayout(1, 1, 0, 8)).apply {
            background = Theme.BASE
            border = BorderFactory.createEmptyBorder(16, 20, 12, 20)
            add(throttlingLabel)
        }

        val footer = JPanel(BorderLayout()).apply {
            background = Theme.BASE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SURFACE0),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
            )
            add(stressButton, BorderLayout.EAST)
        }

        stressButton.addActionListener {
            if (stressExecutor == null) {
                stressExecutor = Executors.newFixedThreadPool(4)
                repeat(4) { stressExecutor?.submit(::spinLoop) }
                stressButton.text = "Stop CPU Stress"
                stressButton.applyStyle(Theme.RED)
            } else {
                stressExecutor?.shutdownNow()
                stressExecutor = null
                stressButton.text = "Start CPU Stress"
                stressButton.applyStyle(Theme.SURFACE0)
            }
        }

        add(topCard, BorderLayout.NORTH)
        add(statsPanel, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    fun update(info: ThermalInfo) {
        if (info == ThermalInfo.INVALID) return
        SwingUtilities.invokeLater {
            bigLabel.text      = info.state.name
            bigLabel.foreground = stateColor(info.state)
            throttlingLabel.text = "Throttling:  ${if (info.isThrottling) "YES" else "NO"}"
        }
    }

    private fun stateColor(state: ThermalState): Color = when (state) {
        ThermalState.NONE, ThermalState.LIGHT -> Theme.GREEN
        ThermalState.MODERATE                 -> Theme.YELLOW
        ThermalState.SEVERE,
        ThermalState.CRITICAL,
        ThermalState.EMERGENCY,
        ThermalState.SHUTDOWN                 -> Theme.PEACH
        ThermalState.UNKNOWN,
        ThermalState.UNSUPPORTED              -> Theme.MUTED
    }

    private fun spinLoop() {
        var x = 0L
        while (!Thread.currentThread().isInterrupted) {
            x = x * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
        }
    }
}
