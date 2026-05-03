package com.smellouk.konitor.jvm

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.gc.GcModule
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.GpuModule
import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.JankModule
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalModule
import com.smellouk.konitor.api.UserEventInfo
import com.smellouk.konitor.jvm.ui.CpuPanel
import com.smellouk.konitor.jvm.ui.FpsPanel
import com.smellouk.konitor.jvm.ui.GcPanel
import com.smellouk.konitor.jvm.ui.GpuPanel
import com.smellouk.konitor.jvm.ui.EventsPanel
import com.smellouk.konitor.jvm.ui.IssuesPanel
import com.smellouk.konitor.jvm.ui.JankPanel
import com.smellouk.konitor.jvm.ui.MemoryPanel
import com.smellouk.konitor.jvm.ui.NetworkPanel
import com.smellouk.konitor.jvm.ui.ThermalPanel
import com.smellouk.konitor.jvm.ui.Theme
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
    SwingUtilities.invokeLater { KonitorDemoWindow().isVisible = true }
}

private class KonitorDemoWindow : JFrame("K|JVM") {
    private val cpuPanel     = CpuPanel()
    private val gpuPanel     = GpuPanel()
    private val fpsPanel     = FpsPanel()
    private val memoryPanel  = MemoryPanel()
    private val eventsPanel  = EventsPanel()
    private val networkPanel = NetworkPanel()
    private val issuesPanel  = IssuesPanel()
    private val jankPanel    = JankPanel()
    private val gcPanel      = GcPanel()
    private val thermalPanel = ThermalPanel()

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
            addTab("  GPU  ",     gpuPanel)
            addTab("  FPS  ",     fpsPanel)
            addTab("  Memory  ",  memoryPanel)
            addTab("  Events  ",  eventsPanel)
            addTab("  Network  ", networkPanel)
            addTab("  Issues  ",  issuesPanel)
            addTab("  Jank  ",    jankPanel)
            addTab("  GC  ",      gcPanel)
            addTab("  Thermal  ", thermalPanel)
        }

        contentPane.add(HeaderPanel(), BorderLayout.NORTH)
        contentPane.add(tabs, BorderLayout.CENTER)

        setupKonitor()
        pack()
        setLocationRelativeTo(null)
    }

    private fun setupKonitor() {
        Konitor.apply {
            install(CpuModule)
            install(GpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)
            install(IssuesModule(anr = AnrConfig()) { crash { chainToPreviousHandler = false } })
            install(JankModule)
            install(GcModule)
            install(ThermalModule)

            addInfoListener<CpuInfo>     { cpuPanel.update(it) }
            addInfoListener<GpuInfo>     { gpuPanel.update(it) }
            addInfoListener<FpsInfo>     { fpsPanel.update(it) }
            addInfoListener<MemoryInfo>     { memoryPanel.update(it) }
            addInfoListener<UserEventInfo>  { eventsPanel.addEvent(it) }
            addInfoListener<NetworkInfo>    { networkPanel.update(it) }
            addInfoListener<IssueInfo>   { issuesPanel.addIssue(it.issue) }
            addInfoListener<JankInfo>    { jankPanel.update(it) }
            addInfoListener<GcInfo>      { gcPanel.update(it) }
            addInfoListener<ThermalInfo> { thermalPanel.update(it) }
        }
        Konitor.start()

        Runtime.getRuntime().addShutdownHook(Thread {
            Konitor.stop()
            Konitor.clear()
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
        val title = "K|JVM"
        val fm = g2.fontMetrics
        val tx = (width - fm.stringWidth(title)) / 2
        g2.drawString(title, tx, height / 2 + fm.ascent / 2 - 2)

        // Subtle dot indicator
        g2.color = Theme.GREEN
        g2.fillOval(tx + fm.stringWidth(title) + 8, height / 2 - 4, 8, 8)
    }
}
