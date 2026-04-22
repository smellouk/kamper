@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios.ui

import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueSpans
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity
import kotlinx.cinterop.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.usleep

class IssuesViewController : UIViewController(nibName = null, bundle = null) {

    private val issues = mutableListOf<Issue>()
    private lateinit var stackView: UIStackView
    private lateinit var scrollView: UIScrollView
    private lateinit var emptyLabel: UILabel
    private lateinit var slowSpanTarget: ActionTarget
    private lateinit var crashTarget: ActionTarget
    private lateinit var clearTarget: ActionTarget

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        emptyLabel = UILabel().apply {
            text = "No issues detected"
            textColor = Theme.MUTED
            font = UIFont.systemFontOfSize(14.0)
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }

        stackView = UIStackView().apply {
            axis = UILayoutConstraintAxisVertical
            spacing = 1.0
            translatesAutoresizingMaskIntoConstraints = false
        }

        scrollView = UIScrollView().apply {
            backgroundColor = Theme.BASE
            translatesAutoresizingMaskIntoConstraints = false
            addSubview(stackView)
        }

        slowSpanTarget = ActionTarget { triggerSlowSpan() }
        crashTarget    = ActionTarget { triggerCrash() }
        clearTarget    = ActionTarget { clearIssues() }

        val slowSpanBtn = makeButton("Slow Span", slowSpanTarget)
        val crashBtn    = makeColoredButton("Crash", UIColor.systemRedColor, crashTarget)
        val clearBtn    = makeButton("Clear", clearTarget)

        val footerScroll = UIScrollView().apply {
            backgroundColor = Theme.MANTLE
            showsHorizontalScrollIndicator = false
            translatesAutoresizingMaskIntoConstraints = false
        }
        val btnStack = UIStackView().apply {
            axis = UILayoutConstraintAxisHorizontal
            spacing = 8.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(slowSpanBtn)
            addArrangedSubview(crashBtn)
            addArrangedSubview(clearBtn)
        }
        footerScroll.addSubview(btnStack)

        val sep = UIView().apply {
            backgroundColor = Theme.SURFACE0
            translatesAutoresizingMaskIntoConstraints = false
        }

        listOf(scrollView, emptyLabel, sep, footerScroll).forEach { view.addSubview(it) }

        val pad = 0.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += footerScroll.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += footerScroll.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += footerScroll.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.bottomAnchor)
        c += footerScroll.heightAnchor.constraintEqualToConstant(52.0)

        c += btnStack.topAnchor.constraintEqualToAnchor(footerScroll.topAnchor, constant = 10.0)
        c += btnStack.leadingAnchor.constraintEqualToAnchor(footerScroll.leadingAnchor, constant = 16.0)
        c += btnStack.trailingAnchor.constraintEqualToAnchor(footerScroll.trailingAnchor, constant = -16.0)
        c += btnStack.bottomAnchor.constraintEqualToAnchor(footerScroll.bottomAnchor, constant = -10.0)
        c += btnStack.heightAnchor.constraintEqualToAnchor(footerScroll.heightAnchor, constant = -20.0)

        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)
        c += sep.bottomAnchor.constraintEqualToAnchor(footerScroll.topAnchor)

        c += scrollView.topAnchor.constraintEqualToAnchor(view.topAnchor, constant = pad)
        c += scrollView.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += scrollView.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += scrollView.bottomAnchor.constraintEqualToAnchor(sep.topAnchor)

        c += stackView.topAnchor.constraintEqualToAnchor(scrollView.topAnchor)
        c += stackView.leadingAnchor.constraintEqualToAnchor(scrollView.leadingAnchor)
        c += stackView.trailingAnchor.constraintEqualToAnchor(scrollView.trailingAnchor)
        c += stackView.bottomAnchor.constraintEqualToAnchor(scrollView.bottomAnchor)
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
        stackView.arrangedSubviews.filterIsInstance<UIView>().forEach { it.removeFromSuperview() }
        issues.forEach { stackView.addArrangedSubview(issueRowView(it)) }
        updateEmpty()
    }

    private fun updateEmpty() {
        emptyLabel.hidden = issues.isNotEmpty()
        scrollView.hidden = issues.isEmpty()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun triggerSlowSpan() {
        CoroutineScope(Dispatchers.Default).launch {
            IssueSpans.measure("ios-demo-op", thresholdMs = 300L) {
                usleep(800_000u)
            }
        }
    }

    private fun triggerCrash() {
        CoroutineScope(Dispatchers.Default).launch {
            throw RuntimeException("Demo crash from K|iOS")
        }
    }
}

private fun issueRowView(issue: Issue): UIView {
    val container = UIView().apply {
        backgroundColor = UIColor(red = 0.192, green = 0.196, blue = 0.267, alpha = 1.0)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val bar = UIView().apply {
        backgroundColor = severityColor(issue.severity)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val typeChip = UILabel().apply {
        text = typeShortName(issue.type)
        textColor = typeColor(issue.type)
        font = UIFont.monospacedSystemFontOfSize(10.0, weight = UIFontWeightBold)
        layer.borderWidth = 1.0
        layer.borderColor = typeColor(issue.type).CGColor
        layer.cornerRadius = 3.0
        translatesAutoresizingMaskIntoConstraints = false
    }

    val sevLabel = UILabel().apply {
        text = issue.severity.name
        textColor = severityColor(issue.severity)
        font = UIFont.systemFontOfSize(11.0)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val timeLabel = UILabel().apply {
        text = fmtTimeIos(issue.timestampMs)
        textColor = Theme.MUTED
        font = UIFont.monospacedSystemFontOfSize(11.0, weight = 0.0)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val msgLabel = UILabel().apply {
        text = issue.message
        textColor = Theme.TEXT
        font = UIFont.systemFontOfSize(12.0)
        numberOfLines = 2
        translatesAutoresizingMaskIntoConstraints = false
    }

    listOf(bar, typeChip, sevLabel, timeLabel, msgLabel).forEach { container.addSubview(it) }

    val details = buildDetailsIos(issue)
    var detailLabel: UILabel? = null
    if (details.isNotEmpty()) {
        detailLabel = UILabel().apply {
            text = details
            textColor = Theme.MUTED
            font = UIFont.monospacedSystemFontOfSize(10.0, weight = 0.0)
            translatesAutoresizingMaskIntoConstraints = false
        }
        container.addSubview(detailLabel)
    }

    val c = mutableListOf<NSLayoutConstraint>()
    c += container.heightAnchor.constraintGreaterThanOrEqualToConstant(56.0)

    c += bar.leadingAnchor.constraintEqualToAnchor(container.leadingAnchor)
    c += bar.topAnchor.constraintEqualToAnchor(container.topAnchor)
    c += bar.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor)
    c += bar.widthAnchor.constraintEqualToConstant(4.0)

    c += typeChip.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
    c += typeChip.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 8.0)

    c += sevLabel.leadingAnchor.constraintEqualToAnchor(typeChip.trailingAnchor, constant = 6.0)
    c += sevLabel.centerYAnchor.constraintEqualToAnchor(typeChip.centerYAnchor)

    c += timeLabel.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -10.0)
    c += timeLabel.centerYAnchor.constraintEqualToAnchor(typeChip.centerYAnchor)

    c += msgLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
    c += msgLabel.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -10.0)
    c += msgLabel.topAnchor.constraintEqualToAnchor(typeChip.bottomAnchor, constant = 4.0)

    if (detailLabel != null) {
        c += detailLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
        c += detailLabel.trailingAnchor.constraintEqualToAnchor(container.trailingAnchor, constant = -10.0)
        c += detailLabel.topAnchor.constraintEqualToAnchor(msgLabel.bottomAnchor, constant = 2.0)
        c += detailLabel.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -8.0)
    } else {
        c += msgLabel.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -8.0)
    }

    NSLayoutConstraint.activateConstraints(c)
    return container
}

private fun makeColoredButton(title: String, color: UIColor, target: ActionTarget): UIButton {
    val btn = UIButton.buttonWithType(UIButtonTypeSystem)
    btn.setTitle(title, forState = UIControlStateNormal)
    btn.setTitleColor(color, forState = UIControlStateNormal)
    btn.backgroundColor = Theme.SURFACE0
    btn.layer.cornerRadius = 8.0
    btn.contentEdgeInsets = UIEdgeInsetsMake(6.0, 12.0, 6.0, 12.0)
    btn.translatesAutoresizingMaskIntoConstraints = false
    btn.addTarget(target, NSSelectorFromString("invoke:"), forControlEvents = UIControlEventTouchUpInside)
    return btn
}

private fun severityColor(s: Severity): UIColor = when (s) {
    Severity.CRITICAL -> Theme.uiColor(0xF38BA8)
    Severity.ERROR    -> Theme.uiColor(0xFAB387)
    Severity.WARNING  -> Theme.uiColor(0xF9E2AF)
    Severity.INFO     -> Theme.uiColor(0xA6E3A1)
}

private fun typeColor(t: IssueType): UIColor = when (t) {
    IssueType.ANR, IssueType.CRASH              -> Theme.uiColor(0xF38BA8)
    IssueType.SLOW_COLD_START,
    IssueType.SLOW_WARM_START,
    IssueType.SLOW_HOT_START                    -> Theme.uiColor(0xFAB387)
    IssueType.DROPPED_FRAME                     -> Theme.uiColor(0xF9E2AF)
    IssueType.SLOW_SPAN                         -> Theme.uiColor(0x89B4FA)
    IssueType.MEMORY_PRESSURE, IssueType.NEAR_OOM -> Theme.uiColor(0xCBA6F7)
    IssueType.STRICT_VIOLATION                  -> Theme.uiColor(0x94E2D5)
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

private fun buildDetailsIos(issue: Issue): String {
    val parts = mutableListOf<String>()
    issue.durationMs?.let { parts.add("${it}ms") }
    issue.threadName?.let { parts.add("thread=$it") }
    issue.details.entries.take(2).forEach { parts.add("${it.key}=${it.value}") }
    return parts.joinToString("  ·  ")
}

private fun fmtTimeIos(ms: Long): String {
    val sec = (ms / 1000) % 86400
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
}
