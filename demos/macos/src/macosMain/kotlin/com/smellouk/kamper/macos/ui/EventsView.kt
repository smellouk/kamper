@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.api.UserEventInfo
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

class EventsView : NSView {

    private val events = mutableListOf<Pair<UserEventInfo, Long>>()
    private val stackView: NSStackView
    private val scrollView: NSScrollView
    private val emptyLabel: NSTextField
    private val loginTarget      = ActionTarget { Kamper.logEvent("user_login") }
    private val purchaseTarget   = ActionTarget { Kamper.logEvent("purchase") }
    private val screenViewTarget = ActionTarget { Kamper.logEvent("screen_view") }
    private val videoTarget      = ActionTarget { triggerVideoPlayback() }
    private val clearTarget      = ActionTarget { clearEvents() }
    private val logTarget        = ActionTarget { handleLogCustom() }
    private val customField: NSTextField
    private var isRecording = false
    private var videoBtn: NSButton? = null

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        emptyLabel = NSTextField.labelWithString("No events logged").apply {
            font = Theme.LABEL_FONT
            textColor = Theme.MUTED
            alignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }

        stackView = NSStackView().apply {
            orientation = NSUserInterfaceLayoutOrientationVertical
            spacing = 1.0
            translatesAutoresizingMaskIntoConstraints = false
        }

        scrollView = NSScrollView().apply {
            setContentView(FlippedClipView(NSMakeRect(0.0, 0.0, 0.0, 0.0)))
            documentView = stackView
            hasVerticalScroller = true
            hasHorizontalScroller = false
            translatesAutoresizingMaskIntoConstraints = false
            backgroundColor = Theme.BASE
        }
        NSLayoutConstraint.activateConstraints(listOf(
            stackView.widthAnchor.constraintEqualToAnchor(scrollView.contentView().widthAnchor)
        ))

        val loginBtn      = makeButton("user_login",   loginTarget)
        val purchaseBtn   = makeButton("purchase",     purchaseTarget)
        val screenViewBtn = makeButton("screen_view",  screenViewTarget)
        val vBtn          = makeButton("video_playback", videoTarget)
        videoBtn = vBtn
        val clearBtn      = makeButton("Clear",        clearTarget)
        val logBtn        = makeButton("LOG",          logTarget)

        customField = NSTextField().apply {
            isEditable = true
            isSelectable = true
            isBordered = true
            drawsBackground = true
            backgroundColor = Theme.SURFACE0
            textColor = Theme.TEXT
            font = NSFont.monospacedSystemFontOfSize(12.0, NSFontWeightRegular)
            placeholderString = "custom event name…"
            translatesAutoresizingMaskIntoConstraints = false
        }

        val btnStack = NSStackView().apply {
            orientation = NSUserInterfaceLayoutOrientationHorizontal
            spacing = 8.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(loginBtn)
            addArrangedSubview(purchaseBtn)
            addArrangedSubview(screenViewBtn)
            addArrangedSubview(vBtn)
            addArrangedSubview(clearBtn)
        }

        val inputStack = NSStackView().apply {
            orientation = NSUserInterfaceLayoutOrientationHorizontal
            spacing = 8.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(customField)
            addArrangedSubview(logBtn)
        }

        val footerStack = NSStackView().apply {
            orientation = NSUserInterfaceLayoutOrientationVertical
            spacing = 6.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(btnStack)
            addArrangedSubview(inputStack)
        }

        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }

        listOf(scrollView, emptyLabel, sep, footerStack).forEach { addSubview(it) }

        val c = mutableListOf<NSLayoutConstraint>()
        val pad = 16.0

        c += footerStack.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += footerStack.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += footerStack.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.bottomAnchor.constraintEqualToAnchor(footerStack.topAnchor, constant = -10.0)

        c += scrollView.topAnchor.constraintEqualToAnchor(topAnchor)
        c += scrollView.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += scrollView.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += scrollView.bottomAnchor.constraintEqualToAnchor(sep.topAnchor)

        c += emptyLabel.centerXAnchor.constraintEqualToAnchor(scrollView.centerXAnchor)
        c += emptyLabel.centerYAnchor.constraintEqualToAnchor(scrollView.centerYAnchor)

        NSLayoutConstraint.activateConstraints(c)
        updateEmpty()
    }

    fun addEvent(info: UserEventInfo) {
        val wallMs = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
        events.add(0, Pair(info, wallMs))
        if (events.size > 200) events.removeAt(events.size - 1)
        CoroutineScope(Dispatchers.Main).launch { refresh() }
    }

    private fun triggerVideoPlayback() {
        if (isRecording) return
        isRecording = true
        videoBtn?.title = "Recording…"
        videoBtn?.isEnabled = false
        val token = Kamper.startEvent("video_playback")
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2_000_000_000L), dispatch_get_main_queue()) {
            Kamper.endEvent(token)
            videoBtn?.title = "video_playback"
            videoBtn?.isEnabled = true
            isRecording = false
        }
    }

    private fun handleLogCustom() {
        val raw = customField.stringValue.trim()
        if (raw.isNotEmpty()) {
            Kamper.logEvent(raw)
            customField.stringValue = ""
        }
    }

    private fun clearEvents() {
        events.clear()
        refresh()
    }

    private fun refresh() {
        stackView.arrangedSubviews.forEach { (it as? NSView)?.removeFromSuperview() }
        events.forEach { (event, wallMs) ->
            val row = eventRowView(event, wallMs)
            stackView.addArrangedSubview(row)
            NSLayoutConstraint.activateConstraints(listOf(
                row.widthAnchor.constraintEqualToAnchor(stackView.widthAnchor)
            ))
        }
        updateEmpty()
    }

    private fun updateEmpty() {
        emptyLabel.setHidden(events.isNotEmpty())
        scrollView.setHidden(events.isEmpty())
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}

private fun fmtTime(ms: Long): String {
    val sec = (ms / 1000) % 86400
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
}

private fun eventRowView(event: UserEventInfo, wallMs: Long): NSView {
    val barColor = if (event.durationMs != null) nsColorForEventsRgb(0x89B4FA) else nsColorForEventsRgb(0xA6E3A1)

    val container = NSView(NSMakeRect(0.0, 0.0, 0.0, 46.0)).apply {
        wantsLayer = true
        layer?.backgroundColor = nsColorForEventsRgb(0x313244).CGColor
        translatesAutoresizingMaskIntoConstraints = false
    }

    val bar = NSView(NSMakeRect(0.0, 0.0, 4.0, 0.0)).apply {
        wantsLayer = true
        layer?.backgroundColor = barColor.CGColor
        translatesAutoresizingMaskIntoConstraints = false
    }

    val nameLabel = NSTextField.labelWithString(event.name).apply {
        textColor = Theme.TEXT
        font = NSFont.monospacedSystemFontOfSize(13.0, NSFontWeightMedium)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val timeLabel = NSTextField.labelWithString(fmtTime(wallMs)).apply {
        textColor = Theme.MUTED
        font = NSFont.monospacedSystemFontOfSize(11.0, NSFontWeightRegular)
        translatesAutoresizingMaskIntoConstraints = false
    }

    listOf(bar, nameLabel, timeLabel).forEach { container.addSubview(it) }

    val c = mutableListOf<NSLayoutConstraint>()
    c += container.heightAnchor.constraintGreaterThanOrEqualToConstant(44.0)

    c += bar.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor)
    c += bar.topAnchor.constraintEqualToAnchor(container.topAnchor)
    c += bar.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor)
    c += bar.widthAnchor.constraintEqualToConstant(4.0)

    c += nameLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
    c += nameLabel.trailingAnchor.constraintEqualToAnchor(timeLabel.leadingAnchor, constant = -8.0)

    c += timeLabel.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -10.0)
    c += timeLabel.centerYAnchor.constraintEqualToAnchor(nameLabel.centerYAnchor)

    event.durationMs?.let { dur ->
        val durLabel = NSTextField.labelWithString("${dur}ms").apply {
            textColor = nsColorForEventsRgb(0x89B4FA)
            font = NSFont.monospacedSystemFontOfSize(11.0, NSFontWeightRegular)
            translatesAutoresizingMaskIntoConstraints = false
        }
        container.addSubview(durLabel)
        c += nameLabel.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 8.0)
        c += durLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
        c += durLabel.topAnchor.constraintEqualToAnchor(nameLabel.bottomAnchor, constant = 2.0)
        c += durLabel.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -8.0)
    } ?: run {
        c += nameLabel.centerYAnchor.constraintEqualToAnchor(container.centerYAnchor)
    }

    NSLayoutConstraint.activateConstraints(c)
    return container
}

private fun nsColorForEventsRgb(rgb: Int): NSColor {
    val r = ((rgb shr 16) and 0xFF) / 255.0
    val g = ((rgb shr 8) and 0xFF) / 255.0
    val b = (rgb and 0xFF) / 255.0
    return NSColor.colorWithCalibratedRed(r, green = g, blue = b, alpha = 1.0)
}
