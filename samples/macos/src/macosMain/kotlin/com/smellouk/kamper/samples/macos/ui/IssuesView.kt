@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.samples.macos.ui

import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueSpans
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*
import platform.posix.usleep

class IssuesView : NSView {

    private val issues = mutableListOf<Issue>()
    private val stackView: NSStackView
    private val scrollView: NSScrollView
    private val emptyLabel: NSTextField
    private val slowSpanTarget = ActionTarget { triggerSlowSpan() }
    private val crashTarget    = ActionTarget { triggerCrash() }
    private val clearTarget    = ActionTarget { clearIssues() }

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        emptyLabel = NSTextField.labelWithString("No issues detected").apply {
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

        val clipView = NSClipView().apply {
            documentView = stackView
            translatesAutoresizingMaskIntoConstraints = false
        }

        scrollView = NSScrollView().apply {
            contentView = clipView
            hasVerticalScroller = true
            hasHorizontalScroller = false
            translatesAutoresizingMaskIntoConstraints = false
            backgroundColor = Theme.BASE
        }

        val slowSpanBtn = makeButton("Slow Span", slowSpanTarget)
        val crashBtn    = makeButton("Crash",     crashTarget)
        val clearBtn    = makeButton("Clear",     clearTarget)

        val btnStack = NSStackView().apply {
            orientation = NSUserInterfaceLayoutOrientationHorizontal
            spacing = 8.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(slowSpanBtn)
            addArrangedSubview(crashBtn)
            addArrangedSubview(clearBtn)
        }

        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 1.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }

        listOf(scrollView, emptyLabel, sep, btnStack).forEach { addSubview(it) }

        val c = mutableListOf<NSLayoutConstraint>()
        val pad = 16.0

        c += btnStack.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += btnStack.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += btnStack.heightAnchor.constraintEqualToConstant(28.0)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)
        c += sep.bottomAnchor.constraintEqualToAnchor(btnStack.topAnchor, constant = -10.0)

        c += scrollView.topAnchor.constraintEqualToAnchor(topAnchor)
        c += scrollView.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += scrollView.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += scrollView.bottomAnchor.constraintEqualToAnchor(sep.topAnchor)

        c += stackView.widthAnchor.constraintEqualToAnchor(scrollView.widthAnchor)

        c += emptyLabel.centerXAnchor.constraintEqualToAnchor(scrollView.centerXAnchor)
        c += emptyLabel.centerYAnchor.constraintEqualToAnchor(scrollView.centerYAnchor)

        NSLayoutConstraint.activateConstraints(c)
        updateEmpty()
    }

    fun addIssue(issue: Issue) {
        issues.add(0, issue)
        if (issues.size > 100) issues.removeAt(issues.size - 1)
        CoroutineScope(Dispatchers.Main).launch { refresh() }
    }

    private fun clearIssues() {
        issues.clear()
        refresh()
    }

    private fun refresh() {
        stackView.arrangedSubviews.forEach { it.removeFromSuperview() }
        issues.forEach { stackView.addArrangedSubview(issueRowView(it)) }
        updateEmpty()
    }

    private fun updateEmpty() {
        emptyLabel.isHidden = issues.isNotEmpty()
        scrollView.isHidden = issues.isEmpty()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun triggerSlowSpan() {
        CoroutineScope(Dispatchers.Default).launch {
            IssueSpans.measure("macos-demo-op", thresholdMs = 300L) {
                usleep(800_000u)
            }
        }
    }

    private fun triggerCrash() {
        CoroutineScope(Dispatchers.Default).launch {
            throw RuntimeException("Demo crash from K|macOS")
        }
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}

private fun issueRowView(issue: Issue): NSView {
    val container = NSView(NSMakeRect(0.0, 0.0, 0.0, 58.0)).apply {
        wantsLayer = true
        layer?.backgroundColor = nsColorForRgb(0x313244).CGColor
        translatesAutoresizingMaskIntoConstraints = false
    }

    val bar = NSView(NSMakeRect(0.0, 0.0, 4.0, 0.0)).apply {
        wantsLayer = true
        layer?.backgroundColor = severityColor(issue.severity).CGColor
        translatesAutoresizingMaskIntoConstraints = false
    }

    val typeLabel = NSTextField.labelWithString(typeShortName(issue.type)).apply {
        textColor = typeColor(issue.type)
        font = NSFont.monospacedSystemFontOfSize(10.0, NSFontWeightBold)
        wantsLayer = true
        layer?.borderWidth = 1.0
        layer?.borderColor = typeColor(issue.type).CGColor
        layer?.cornerRadius = 3.0
        translatesAutoresizingMaskIntoConstraints = false
    }

    val sevLabel = NSTextField.labelWithString(issue.severity.name).apply {
        textColor = severityColor(issue.severity)
        font = NSFont.systemFontOfSize(11.0)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val timeLabel = NSTextField.labelWithString(fmtTimeMacos(issue.timestampMs)).apply {
        textColor = Theme.MUTED
        font = NSFont.monospacedSystemFontOfSize(11.0, NSFontWeightRegular)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val msgLabel = NSTextField.labelWithString(issue.message).apply {
        textColor = Theme.TEXT
        font = NSFont.systemFontOfSize(12.0)
        translatesAutoresizingMaskIntoConstraints = false
    }

    listOf(bar, typeLabel, sevLabel, timeLabel, msgLabel).forEach { container.addSubview(it) }

    val details = buildDetailsMacos(issue)
    var detailLabel: NSTextField? = null
    if (details.isNotEmpty()) {
        detailLabel = NSTextField.labelWithString(details).apply {
            textColor = Theme.MUTED
            font = NSFont.monospacedSystemFontOfSize(10.0, NSFontWeightRegular)
            translatesAutoresizingMaskIntoConstraints = false
        }
        container.addSubview(detailLabel)
    }

    val c = mutableListOf<NSLayoutConstraint>()
    c += container.heightAnchor.constraintGreaterThanOrEqualToConstant(54.0)

    c += bar.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor)
    c += bar.topAnchor.constraintEqualToAnchor(container.topAnchor)
    c += bar.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor)
    c += bar.widthAnchor.constraintEqualToConstant(4.0)

    c += typeLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
    c += typeLabel.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 8.0)

    c += sevLabel.leadingAnchor.constraintEqualToAnchor(typeLabel.trailingAnchor, constant = 6.0)
    c += sevLabel.centerYAnchor.constraintEqualToAnchor(typeLabel.centerYAnchor)

    c += timeLabel.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -10.0)
    c += timeLabel.centerYAnchor.constraintEqualToAnchor(typeLabel.centerYAnchor)

    c += msgLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
    c += msgLabel.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -10.0)
    c += msgLabel.topAnchor.constraintEqualToAnchor(typeLabel.bottomAnchor, constant = 4.0)

    if (detailLabel != null) {
        c += detailLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
        c += detailLabel.topAnchor.constraintEqualToAnchor(msgLabel.bottomAnchor, constant = 2.0)
        c += detailLabel.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -8.0)
    } else {
        c += msgLabel.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -8.0)
    }

    NSLayoutConstraint.activateConstraints(c)
    return container
}

private fun nsColorForRgb(rgb: Int): NSColor {
    val r = ((rgb shr 16) and 0xFF) / 255.0
    val g = ((rgb shr 8) and 0xFF) / 255.0
    val b = (rgb and 0xFF) / 255.0
    return NSColor.colorWithCalibratedRed(r, green = g, blue = b, alpha = 1.0)
}

private fun severityColor(s: Severity): NSColor = when (s) {
    Severity.CRITICAL -> nsColorForRgb(0xF38BA8)
    Severity.ERROR    -> nsColorForRgb(0xFAB387)
    Severity.WARNING  -> nsColorForRgb(0xF9E2AF)
    Severity.INFO     -> nsColorForRgb(0xA6E3A1)
}

private fun typeColor(t: IssueType): NSColor = when (t) {
    IssueType.ANR, IssueType.CRASH              -> nsColorForRgb(0xF38BA8)
    IssueType.SLOW_COLD_START,
    IssueType.SLOW_WARM_START,
    IssueType.SLOW_HOT_START                    -> nsColorForRgb(0xFAB387)
    IssueType.DROPPED_FRAME                     -> nsColorForRgb(0xF9E2AF)
    IssueType.SLOW_SPAN                         -> nsColorForRgb(0x89B4FA)
    IssueType.MEMORY_PRESSURE, IssueType.NEAR_OOM -> nsColorForRgb(0xCBA6F7)
    IssueType.STRICT_VIOLATION                  -> nsColorForRgb(0x94E2D5)
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

private fun buildDetailsMacos(issue: Issue): String {
    val parts = mutableListOf<String>()
    issue.durationMs?.let { parts.add("${it}ms") }
    issue.threadName?.let { parts.add("thread=$it") }
    issue.details.entries.take(2).forEach { parts.add("${it.key}=${it.value}") }
    return parts.joinToString("  ·  ")
}

private fun fmtTimeMacos(ms: Long): String {
    val sec = (ms / 1000) % 86400
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
}
