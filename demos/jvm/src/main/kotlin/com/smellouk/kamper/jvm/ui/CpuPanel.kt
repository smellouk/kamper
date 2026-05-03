package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridLayout
import java.util.concurrent.Executors
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class CpuPanel : JPanel(BorderLayout(0, 0)) {

    private data class Metric(val label: String, val color: Color)

    private val metrics = listOf(
        Metric("Total",   Theme.BLUE),
        Metric("App",     Theme.GREEN),
        Metric("User",    Theme.YELLOW),
        Metric("System",  Theme.PEACH),
        Metric("IO Wait", Theme.MAUVE)
    )

    private val rows = metrics.associate { it.label to MetricRow(it.label, it.color) }

    private val loadButton = JButton("Start CPU Load").applyStyle(Theme.SURFACE0)
    private var loadExecutor: java.util.concurrent.ExecutorService? = null

    init {
        background = Theme.BASE

        val rowsPanel = JPanel().apply {
            background = Theme.BASE
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(20, 20, 12, 20)
            rows.values.forEach { row ->
                add(row)
                add(Box.createVerticalStrut(6))
            }
        }

        val footer = JPanel(BorderLayout()).apply {
            background = Theme.BASE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SURFACE0),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
            )
            add(loadButton, BorderLayout.EAST)
        }

        loadButton.addActionListener {
            if (loadExecutor == null) {
                Kamper.logEvent("cpu_load_start")
                loadExecutor = Executors.newFixedThreadPool(LOAD_THREADS)
                repeat(LOAD_THREADS) { loadExecutor?.submit(::spinLoop) }
                loadButton.text = "Stop CPU Load"
                loadButton.applyStyle(Theme.RED)
            } else {
                Kamper.logEvent("cpu_load_stop")
                loadExecutor?.shutdownNow()
                loadExecutor = null
                loadButton.text = "Start CPU Load"
                loadButton.applyStyle(Theme.SURFACE0)
            }
        }

        add(rowsPanel, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    fun update(info: CpuInfo) {
        if (info == CpuInfo.INVALID) return
        SwingUtilities.invokeLater {
            rows["Total"]?.apply  { fraction = info.totalUseRatio.toFloat(); valueText = "%.1f%%".format(info.totalUseRatio * 100) }
            rows["App"]?.apply    { fraction = info.appRatio.toFloat();      valueText = "%.1f%%".format(info.appRatio * 100) }
            rows["User"]?.apply   { fraction = info.userRatio.toFloat();     valueText = "%.1f%%".format(info.userRatio * 100) }
            rows["System"]?.apply { fraction = info.systemRatio.toFloat();   valueText = "%.1f%%".format(info.systemRatio * 100) }
            rows["IO Wait"]?.apply{ fraction = info.ioWaitRatio.toFloat();   valueText = "%.1f%%".format(info.ioWaitRatio * 100) }
        }
    }

    private fun spinLoop() {
        var x = 0L
        while (!Thread.currentThread().isInterrupted) {
            x = x * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L
        }
    }

    private companion object {
        const val LOAD_THREADS = 4
    }
}
