package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import java.net.URL
import java.util.concurrent.Executors
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class NetworkPanel : JPanel(BorderLayout(0, 0)) {

    private val rxRow = MetricRow("Download", Theme.TEAL,  Theme.SURFACE0)
    private val txRow = MetricRow("Upload",   Theme.MAUVE, Theme.SURFACE0)

    private val rxDetail  = statValue()
    private val txDetail  = statValue()
    private val statusLabel = JLabel("").apply {
        foreground = Theme.MUTED
        font = Theme.HINT_FONT
        horizontalAlignment = SwingConstants.RIGHT
    }

    private val downloadButton = JButton("Test Download").applyStyle(Theme.SURFACE0)
    private var peakRx = 0f
    private var peakTx = 0f

    init {
        background = Theme.BASE

        val rows = JPanel().apply {
            background = Theme.BASE
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(20, 20, 12, 20)

            add(sectionLabel("System Traffic  (per interval)"))
            add(Box.createVerticalStrut(8))
            add(rxRow)
            add(rxDetail)
            add(Box.createVerticalStrut(14))
            add(txRow)
            add(txDetail)
            add(Box.createVerticalStrut(12))
            add(JLabel("<html><i>Per-app traffic is Android-only.</i></html>").apply {
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
            add(statusLabel, BorderLayout.CENTER)
            add(downloadButton, BorderLayout.EAST)
        }

        downloadButton.addActionListener { triggerDownload() }

        add(rows, BorderLayout.CENTER)
        add(footer, BorderLayout.SOUTH)
    }

    fun update(info: NetworkInfo) {
        if (info == NetworkInfo.INVALID) return
        SwingUtilities.invokeLater {
            if (info == NetworkInfo.NOT_SUPPORTED) {
                rxDetail.text = "Not supported on this OS"
                txDetail.text = "Not supported on this OS"
                return@invokeLater
            }
            peakRx = maxOf(peakRx, info.rxSystemTotalInMb)
            peakTx = maxOf(peakTx, info.txSystemTotalInMb)

            // Normalise bar against a rolling 10 MB/s scale
            val scale = maxOf(peakRx, peakTx, MIN_SCALE_MB)
            rxRow.fraction  = (info.rxSystemTotalInMb / scale).coerceIn(0f, 1f)
            rxRow.valueText = formatSpeed(info.rxSystemTotalInMb)
            txRow.fraction  = (info.txSystemTotalInMb / scale).coerceIn(0f, 1f)
            txRow.valueText = formatSpeed(info.txSystemTotalInMb)

            rxDetail.text = "%.3f MB/interval   peak %.2f MB".format(info.rxSystemTotalInMb, peakRx)
            txDetail.text = "%.3f MB/interval   peak %.2f MB".format(info.txSystemTotalInMb, peakTx)
        }
    }

    private fun triggerDownload() {
        Kamper.logEvent("network_download_test")
        statusLabel.text = "Fetching 20 MB…"
        downloadButton.isEnabled = false
        Executors.newSingleThreadExecutor().submit {
            runCatching {
                URL("https://speed.cloudflare.com/__down?bytes=20000000")
                    .openStream().use { it.readBytes() }
            }
            SwingUtilities.invokeLater {
                statusLabel.text = "Done"
                downloadButton.isEnabled = true
            }
        }
    }

    private fun formatSpeed(mb: Float): String = when {
        mb >= 1f    -> "%.2f MB".format(mb)
        mb >= 0.01f -> "%.0f KB".format(mb * 1024)
        else        -> "< 10 KB"
    }

    private fun statValue() = JLabel("—").apply {
        foreground = Theme.MUTED
        font = Font("Monospaced", Font.PLAIN, 11)
    }

    private fun sectionLabel(text: String) = JLabel(text).apply {
        foreground = Theme.SUBTEXT
        font = Font("SansSerif", Font.BOLD, 12)
    }

    private companion object {
        const val MIN_SCALE_MB = 1f
    }
}
