package com.smellouk.kamper.samples.jvm

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import com.smellouk.kamper.samples.jvm.ui.CpuPanel
import com.smellouk.kamper.samples.jvm.ui.FpsPanel
import com.smellouk.kamper.samples.jvm.ui.IssuesPanel
import com.smellouk.kamper.samples.jvm.ui.MemoryPanel
import com.smellouk.kamper.samples.jvm.ui.NetworkPanel
import com.smellouk.kamper.samples.jvm.ui.Theme
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

fun main() {
    Theme.applyGlobals()
    SwingUtilities.invokeLater { KamperDemoWindow().isVisible = true }
}

private class KamperDemoWindow : JFrame("K|JVM") {
    private val cpuPanel     = CpuPanel()
    private val fpsPanel     = FpsPanel()
    private val memoryPanel  = MemoryPanel()
    private val networkPanel = NetworkPanel()
    private val issuesPanel  = IssuesPanel()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        preferredSize = Dimension(680, 520)
        minimumSize   = Dimension(560, 440)
        contentPane.background = Theme.BASE

        val tabs = JTabbedPane().apply {
            background = Theme.BASE
            foreground = Theme.TEXT
            font       = Font("SansSerif", Font.PLAIN, 13)
            tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
            addTab("  CPU  ",     cpuPanel)
            addTab("  FPS  ",     fpsPanel)
            addTab("  Memory  ",  memoryPanel)
            addTab("  Network  ", networkPanel)
            addTab("  Issues  ",  issuesPanel)
        }

        contentPane.add(HeaderPanel(), BorderLayout.NORTH)
        contentPane.add(tabs, BorderLayout.CENTER)

        setupKamper()
        pack()
        setLocationRelativeTo(null)
    }

    private fun setupKamper() {
        Kamper.apply {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)
            install(IssuesModule(anr = AnrConfig()) { crash { chainToPreviousHandler = false } })

            addInfoListener<CpuInfo>     { cpuPanel.update(it) }
            addInfoListener<FpsInfo>     { fpsPanel.update(it) }
            addInfoListener<MemoryInfo>  { memoryPanel.update(it) }
            addInfoListener<NetworkInfo> { networkPanel.update(it) }
            addInfoListener<IssueInfo>   { issuesPanel.addIssue(it.issue) }
        }
        Kamper.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            Kamper.stop()
            Kamper.clear()
        })
    }
}

private class HeaderPanel : JPanel() {
    init {
        preferredSize = Dimension(0, 52)
        isOpaque = false
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB)

        // Gradient background
        g2.paint = GradientPaint(0f, 0f, Theme.MANTLE, width.toFloat(), 0f, Theme.BASE)
        g2.fillRect(0, 0, width, height)

        // Bottom border line
        g2.color = Theme.SURFACE0
        g2.drawLine(0, height - 1, width, height - 1)

        // Title
        g2.font = Font("SansSerif", Font.BOLD, 15)
        g2.color = Theme.BLUE
        val title = "Kamper Performance Monitor"
        val fm = g2.fontMetrics
        g2.drawString(title, (width - fm.stringWidth(title)) / 2, height / 2 + fm.ascent / 2 - 2)

        // Subtle dot indicator
        g2.color = Theme.GREEN
        g2.fillOval(width / 2 + fm.stringWidth(title) / 2 + 8, height / 2 - 4, 8, 8)
    }
}
