package com.smellouk.konitor.jvm.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.api.UserEventInfo
import com.smellouk.konitor.jvm.ui.Theme.applyStyle
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

class EventsPanel : JPanel(BorderLayout(0, 0)) {

    private val eventListPanel = JPanel().apply {
        background = Theme.BASE
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(8, 0, 8, 0)
    }

    private val emptyLabel = JLabel("No events logged", SwingConstants.CENTER).apply {
        foreground = Theme.MUTED
        font = Font("SansSerif", Font.PLAIN, 14)
        alignmentX = Component.CENTER_ALIGNMENT
    }

    private val events = mutableListOf<Pair<UserEventInfo, Long>>()

    init {
        background = Theme.BASE

        val scrollPane = JScrollPane(eventListPanel).apply {
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
        val loginBtn      = JButton("user_login").applyStyle(Theme.BLUE)
        val purchaseBtn   = JButton("purchase").applyStyle(Theme.BLUE)
        val screenViewBtn = JButton("screen_view").applyStyle(Theme.BLUE)
        val videoBtn      = JButton("video_playback").applyStyle(Theme.BLUE)
        val clearBtn      = JButton("Clear").applyStyle(Theme.SURFACE0)

        loginBtn.addActionListener      { Konitor.logEvent("user_login") }
        purchaseBtn.addActionListener   { Konitor.logEvent("purchase") }
        screenViewBtn.addActionListener { Konitor.logEvent("screen_view") }
        clearBtn.addActionListener      { clearEvents() }
        videoBtn.addActionListener {
            videoBtn.isEnabled = false
            videoBtn.text = "Recording…"
            val token = Konitor.startEvent("video_playback")
            Thread {
                try { Thread.sleep(2_000L) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
                Konitor.endEvent(token)
                SwingUtilities.invokeLater {
                    videoBtn.text = "video_playback"
                    videoBtn.isEnabled = true
                }
            }.apply { isDaemon = true }.start()
        }

        val customField = JTextField(16).apply {
            background = Theme.BASE
            foreground = Theme.TEXT
            font = Font("Monospaced", Font.PLAIN, 12)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.SURFACE0),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
            )
            maximumSize = Dimension(200, 28)
        }
        val logBtn = JButton("LOG").applyStyle(Theme.BLUE)
        logBtn.addActionListener {
            val raw = customField.text?.trim().orEmpty()
            if (raw.isNotEmpty()) {
                Konitor.logEvent(raw)
                customField.text = ""
            }
        }
        customField.addActionListener {
            val raw = customField.text?.trim().orEmpty()
            if (raw.isNotEmpty()) {
                Konitor.logEvent(raw)
                customField.text = ""
            }
        }

        return JPanel().apply {
            background = Theme.MANTLE
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.SURFACE0),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
            )
            add(loginBtn)
            add(Box.createHorizontalStrut(8))
            add(purchaseBtn)
            add(Box.createHorizontalStrut(8))
            add(screenViewBtn)
            add(Box.createHorizontalStrut(8))
            add(videoBtn)
            add(Box.createHorizontalStrut(8))
            add(clearBtn)
            add(Box.createHorizontalStrut(16))
            add(customField)
            add(Box.createHorizontalStrut(8))
            add(logBtn)
            add(Box.createHorizontalGlue())
        }
    }

    fun addEvent(info: UserEventInfo) {
        SwingUtilities.invokeLater {
            events.add(0, Pair(info, System.currentTimeMillis()))
            if (events.size > 200) events.removeAt(events.size - 1)
            refresh()
        }
    }

    private fun clearEvents() {
        events.clear()
        refresh()
    }

    private fun refresh() {
        eventListPanel.removeAll()
        if (events.isEmpty()) {
            eventListPanel.add(Box.createVerticalStrut(60))
            eventListPanel.add(emptyLabel)
        } else {
            events.forEach { (event, wallMs) ->
                eventListPanel.add(EventRow(event, wallMs))
                val sep = JPanel().apply {
                    background = Theme.SURFACE0
                    maximumSize = Dimension(Int.MAX_VALUE, 1)
                    preferredSize = Dimension(0, 1)
                }
                eventListPanel.add(sep)
            }
        }
        eventListPanel.revalidate()
        eventListPanel.repaint()
    }
}

private class EventRow(private val event: UserEventInfo, private val wallMs: Long) : JPanel(BorderLayout(0, 0)) {

    init {
        background = Theme.BASE
        maximumSize = Dimension(Int.MAX_VALUE, 52)
        preferredSize = Dimension(0, 52)

        val barColor = if (event.durationMs != null) Color(0x89, 0xB4, 0xFA) else Color(0xA6, 0xE3, 0xA1)
        val bar = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                g.color = barColor
                g.fillRect(0, 0, width, height)
            }
        }.apply { preferredSize = Dimension(4, 0) }

        add(bar, BorderLayout.WEST)
        add(buildContent(), BorderLayout.CENTER)
    }

    private fun buildContent(): JPanel {
        val nameLabel = JLabel(event.name).apply {
            foreground = Theme.TEXT
            font = Font("Monospaced", Font.BOLD, 13)
        }

        val timeLabel = JLabel(TS_FMT.format(Date(wallMs))).apply {
            foreground = Theme.MUTED
            font = Font("Monospaced", Font.PLAIN, 11)
        }

        val detailText = event.durationMs?.let { "(${it}ms)" } ?: ""
        val detailLabel = if (detailText.isNotEmpty()) JLabel(detailText).apply {
            foreground = Color(0x89, 0xB4, 0xFA)
            font = Font("Monospaced", Font.PLAIN, 11)
        } else null

        val topRow = JPanel().apply {
            background = Theme.BASE
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(nameLabel)
            add(Box.createHorizontalGlue())
            add(timeLabel)
        }

        return JPanel().apply {
            background = Theme.BASE
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(6, 10, 6, 10)
            add(topRow)
            if (detailLabel != null) {
                add(Box.createVerticalStrut(2))
                add(detailLabel)
            }
        }
    }

    companion object {
        private val TS_FMT = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }
}
