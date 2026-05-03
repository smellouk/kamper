package com.smellouk.kamper.jvm.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueSpans
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity
import com.smellouk.kamper.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executors
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class IssuesPanel : JPanel(BorderLayout(0, 0)) {

    private val issueListPanel = JPanel().apply {
        background = Theme.BASE
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(8, 0, 8, 0)
    }

    private val emptyLabel = JLabel("No issues detected", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("SansSerif", Font.PLAIN, 14)
        alignmentX = Component.CENTER_ALIGNMENT
    }

    private val issues = mutableListOf<Issue>()
    private val timeFmt = SimpleDateFormat("HH:mm:ss")

    init {
        background = Theme.BASE

        val scrollPane = JScrollPane(issueListPanel).apply {
            background = Theme.BASE
            viewport.background = Theme.BASE
            border = null
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        }

        add(scrollPane, BorderLayout.CENTER)
        add(buildFooter(), BorderLayout.SOUTH)
        refresh()
    }

    private fun buildFooter(): JPanel {
        val slowSpanBtn = JButton("Slow Span").applyStyle(Color(0x3A, 0x5A, 0x8A))
        val crashBtn    = JButton("Crash").applyStyle(Color(0x7A, 0x3A, 0x4A))
        val clearBtn    = JButton("Clear").applyStyle(Theme.SURFACE0)

        slowSpanBtn.addActionListener {
            Kamper.logEvent("issue_slow_span")
            Executors.newSingleThreadExecutor().submit {
                IssueSpans.measure("jvm-demo-op", thresholdMs = 300L) {
                    Thread.sleep(800)
                }
            }
        }
        crashBtn.addActionListener {
            Kamper.logEvent("issue_crash_trigger")
            Thread { throw RuntimeException("Demo crash from K|JVM") }
                .also { it.isDaemon = true; it.start() }
        }
        clearBtn.addActionListener {
            Kamper.logEvent("issues_clear")
            clearIssues()
        }

        return JPanel().apply {
            background = Theme.MANTLE
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SURFACE0),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
            )
            add(slowSpanBtn)
            add(Box.createHorizontalStrut(8))
            add(crashBtn)
            add(Box.createHorizontalStrut(8))
            add(clearBtn)
            add(Box.createHorizontalGlue())
        }
    }

    fun addIssue(issue: Issue) {
        SwingUtilities.invokeLater {
            issues.add(0, issue)
            if (issues.size > 100) issues.removeAt(issues.size - 1)
            refresh()
        }
    }

    private fun clearIssues() {
        issues.clear()
        refresh()
    }

    private fun refresh() {
        issueListPanel.removeAll()
        if (issues.isEmpty()) {
            issueListPanel.add(Box.createVerticalStrut(60))
            issueListPanel.add(emptyLabel)
        } else {
            issues.forEach { issue ->
                issueListPanel.add(IssueRow(issue, timeFmt))
                val sep = JPanel().apply {
                    background = Theme.SURFACE0
                    maximumSize = Dimension(Int.MAX_VALUE, 1)
                    preferredSize = Dimension(0, 1)
                }
                issueListPanel.add(sep)
            }
        }
        issueListPanel.revalidate()
        issueListPanel.repaint()
    }
}

private class IssueRow(private val issue: Issue, private val fmt: SimpleDateFormat) : JPanel(BorderLayout(0, 0)) {

    init {
        background = Theme.BASE
        maximumSize = Dimension(Int.MAX_VALUE, 64)
        preferredSize = Dimension(0, 64)

        val bar = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                g.color = severityColor(issue.severity)
                g.fillRect(0, 0, width, height)
            }
        }.apply { preferredSize = Dimension(4, 0) }

        add(bar, BorderLayout.WEST)
        add(buildContent(), BorderLayout.CENTER)
    }

    private fun buildContent(): JPanel {
        val typeLabel = JLabel(typeShortName(issue.type)).apply {
            foreground = typeColor(issue.type)
            font = Font("Monospaced", Font.BOLD, 10)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(typeColor(issue.type).darker(), 1),
                BorderFactory.createEmptyBorder(1, 4, 1, 4)
            )
        }
        val sevLabel = JLabel(issue.severity.name).apply {
            foreground = severityColor(issue.severity)
            font = Font("SansSerif", Font.PLAIN, 11)
        }
        val timeLabel = JLabel(fmt.format(Date(issue.timestampMs))).apply {
            foreground = Theme.MUTED
            font = Font("Monospaced", Font.PLAIN, 11)
        }

        val topRow = JPanel().apply {
            background = Theme.BASE
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(typeLabel)
            add(Box.createHorizontalStrut(6))
            add(sevLabel)
            add(Box.createHorizontalGlue())
            add(timeLabel)
        }

        val msgLabel = JLabel("<html>${issue.message}</html>").apply {
            foreground = Theme.TEXT
            font = Font("SansSerif", Font.PLAIN, 12)
        }

        val details = buildDetails(issue)

        return JPanel().apply {
            background = Theme.BASE
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(6, 10, 6, 10)
            add(topRow)
            add(Box.createVerticalStrut(2))
            add(msgLabel)
            if (details.isNotEmpty()) {
                add(JLabel(details).apply {
                    foreground = Theme.MUTED
                    font = Font("Monospaced", Font.PLAIN, 11)
                })
            }
        }
    }
}

private fun severityColor(s: Severity): Color = when (s) {
    Severity.CRITICAL -> Color(0xF3, 0x8B, 0xA8)
    Severity.ERROR    -> Color(0xFA, 0xB3, 0x87)
    Severity.WARNING  -> Color(0xF9, 0xE2, 0xAF)
    Severity.INFO     -> Color(0xA6, 0xE3, 0xA1)
}

private fun typeColor(t: IssueType): Color = when (t) {
    IssueType.ANR, IssueType.CRASH                    -> Color(0xF3, 0x8B, 0xA8)
    IssueType.SLOW_COLD_START,
    IssueType.SLOW_WARM_START,
    IssueType.SLOW_HOT_START                          -> Color(0xFA, 0xB3, 0x87)
    IssueType.DROPPED_FRAME                           -> Color(0xF9, 0xE2, 0xAF)
    IssueType.SLOW_SPAN                               -> Color(0x89, 0xB4, 0xFA)
    IssueType.MEMORY_PRESSURE, IssueType.NEAR_OOM     -> Color(0xCB, 0xA6, 0xF7)
    IssueType.STRICT_VIOLATION                        -> Color(0x94, 0xE2, 0xD5)
}

private fun typeShortName(t: IssueType): String = when (t) {
    IssueType.ANR              -> "ANR"
    IssueType.SLOW_COLD_START  -> "COLD"
    IssueType.SLOW_WARM_START  -> "WARM"
    IssueType.SLOW_HOT_START   -> "HOT"
    IssueType.DROPPED_FRAME    -> "JANK"
    IssueType.SLOW_SPAN        -> "SPAN"
    IssueType.MEMORY_PRESSURE  -> "MEM"
    IssueType.NEAR_OOM         -> "OOM"
    IssueType.CRASH            -> "CRASH"
    IssueType.STRICT_VIOLATION -> "STRICT"
}

private fun buildDetails(issue: Issue): String {
    val parts = mutableListOf<String>()
    issue.durationMs?.let { parts.add("${it}ms") }
    issue.threadName?.let { parts.add("thread=$it") }
    issue.details.entries.take(2).forEach { parts.add("${it.key}=${it.value}") }
    return parts.joinToString("  ·  ")
}
