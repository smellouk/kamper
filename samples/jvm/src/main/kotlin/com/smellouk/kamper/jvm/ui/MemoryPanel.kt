package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class MemoryPanel : JPanel(BorderLayout(0, 0)) {

    private val heapRow  = MetricRow("Heap Used", Theme.GREEN)
    private val ramRow   = MetricRow("RAM Used",  Theme.BLUE)

    private val heapDetail = detailLabel()
    private val ramDetail  = detailLabel()
    private val lowMemBadge = JLabel("").apply {
        foreground = Theme.RED
        font = Font("SansSerif", Font.BOLD, 12)
        horizontalAlignment = SwingConstants.RIGHT
    }

    private val allocations = mutableListOf<ByteArray>()
    private val allocButton = JButton("Alloc 32 MB").applyStyle(Theme.SURFACE0)
    private val gcButton    = JButton("Force GC").applyStyle(Theme.SURFACE0)

    init {
        background = Theme.BASE

        val rows = JPanel().apply {
            background = Theme.BASE
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(20, 20, 12, 20)

            add(sectionLabel("Heap Memory"))
            add(Box.createVerticalStrut(6))
            add(heapRow)
            add(heapDetail)
            add(Box.createVerticalStrut(18))
            add(sectionLabel("System RAM"))
            add(Box.createVerticalStrut(6))
            add(ramRow)
            add(ramDetail)
            add(Box.createVerticalStrut(8))
            add(JLabel("<html><i>PSS metrics: Android only</i></html>").apply {
                foreground = Theme.MUTED
                font = Theme.HINT_FONT
            })
        }

        val footer = JPanel(BorderLayout(8, 0)).apply {
            background = Theme.BASE
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SURFACE0),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
            )
            add(lowMemBadge, BorderLayout.CENTER)
            add(JPanel(GridLayout(1, 2, 8, 0)).apply {
                background = Theme.BASE
                add(allocButton)
                add(gcButton)
            }, BorderLayout.EAST)
        }

        allocButton.addActionListener { allocations.add(ByteArray(32 * 1024 * 1024)) }
        gcButton.addActionListener    { allocations.clear(); System.gc() }

        add(rows, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    fun update(info: MemoryInfo) {
        if (info == MemoryInfo.INVALID) return
        SwingUtilities.invokeLater {
            with(info.heapMemoryInfo) {
                val frac = if (maxMemoryInMb > 0) allocatedInMb / maxMemoryInMb else 0f
                heapRow.fraction  = frac
                heapRow.valueText = "%.0f%%".format(frac * 100)
                heapDetail.text   = "%.1f MB used  /  %.1f MB max".format(allocatedInMb, maxMemoryInMb)
            }
            with(info.ramInfo) {
                val usedRam = totalRamInMb - availableRamInMb
                val frac    = if (totalRamInMb > 0) usedRam / totalRamInMb else 0f
                ramRow.fraction  = frac
                ramRow.valueText = "%.0f%%".format(frac * 100)
                ramDetail.text   = "%.0f MB used  /  %.0f MB total".format(usedRam, totalRamInMb)
                lowMemBadge.text = if (isLowMemory) "⚠ Low Memory" else ""
            }
        }
    }

    private fun detailLabel() = JLabel("—").apply {
        foreground = Theme.MUTED
        font = Font("Monospaced", Font.PLAIN, 11)
    }

    private fun sectionLabel(text: String) = JLabel(text).apply {
        foreground = Theme.SUBTEXT
        font = Font("SansSerif", Font.BOLD, 12)
    }
}
