@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.api.UserEventInfo
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

class EventsViewController : UIViewController(nibName = null, bundle = null) {

    private val events = mutableListOf<Pair<UserEventInfo, Long>>()
    private lateinit var stackView: UIStackView
    private lateinit var scrollView: UIScrollView
    private lateinit var emptyLabel: UILabel
    private lateinit var loginTarget: ActionTarget
    private lateinit var purchaseTarget: ActionTarget
    private lateinit var screenViewTarget: ActionTarget
    private lateinit var videoTarget: ActionTarget
    private lateinit var clearTarget: ActionTarget
    private lateinit var logTarget: ActionTarget
    private lateinit var customInput: UITextField
    private var isRecording = false
    private var videoBtn: UIButton? = null

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        emptyLabel = UILabel().apply {
            text = "No events logged"
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

        loginTarget      = ActionTarget { Kamper.logEvent("user_login") }
        purchaseTarget   = ActionTarget { Kamper.logEvent("purchase") }
        screenViewTarget = ActionTarget { Kamper.logEvent("screen_view") }
        videoTarget      = ActionTarget { triggerVideoPlayback() }
        clearTarget      = ActionTarget { clearEvents() }
        logTarget        = ActionTarget { handleLogCustom() }

        val loginBtn      = makeButton("user_login",   loginTarget)
        val purchaseBtn   = makeButton("purchase",     purchaseTarget)
        val screenViewBtn = makeButton("screen_view",  screenViewTarget)
        val vBtn          = makeButton("video_playback", videoTarget)
        videoBtn = vBtn
        val clearBtn      = makeButton("Clear",        clearTarget)
        val logBtn        = makeButton("LOG",          logTarget)

        customInput = UITextField().apply {
            backgroundColor = Theme.SURFACE0
            textColor = Theme.TEXT
            font = UIFont.monospacedSystemFontOfSize(13.0, weight = 0.0)
            placeholder = "custom event name…"
            translatesAutoresizingMaskIntoConstraints = false
        }

        val footerScroll = UIScrollView().apply {
            backgroundColor = Theme.MANTLE
            showsHorizontalScrollIndicator = false
            translatesAutoresizingMaskIntoConstraints = false
        }
        val btnStack = UIStackView().apply {
            axis = UILayoutConstraintAxisHorizontal
            spacing = 8.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(loginBtn)
            addArrangedSubview(purchaseBtn)
            addArrangedSubview(screenViewBtn)
            addArrangedSubview(vBtn)
            addArrangedSubview(clearBtn)
        }
        val inputStack = UIStackView().apply {
            axis = UILayoutConstraintAxisHorizontal
            spacing = 8.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(customInput)
            addArrangedSubview(logBtn)
        }
        val outerStack = UIStackView().apply {
            axis = UILayoutConstraintAxisVertical
            spacing = 8.0
            translatesAutoresizingMaskIntoConstraints = false
            addArrangedSubview(btnStack)
            addArrangedSubview(inputStack)
        }
        footerScroll.addSubview(outerStack)

        val sep = UIView().apply {
            backgroundColor = Theme.SURFACE0
            translatesAutoresizingMaskIntoConstraints = false
        }

        listOf(scrollView, emptyLabel, sep, footerScroll).forEach { view.addSubview(it) }

        val c = mutableListOf<NSLayoutConstraint>()

        c += footerScroll.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += footerScroll.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += footerScroll.bottomAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.bottomAnchor)
        c += footerScroll.heightAnchor.constraintEqualToConstant(100.0)

        c += outerStack.topAnchor.constraintEqualToAnchor(footerScroll.topAnchor, constant = 10.0)
        c += outerStack.leadingAnchor.constraintEqualToAnchor(footerScroll.leadingAnchor, constant = 16.0)
        c += outerStack.trailingAnchor.constraintEqualToAnchor(footerScroll.trailingAnchor, constant = -16.0)
        c += outerStack.bottomAnchor.constraintEqualToAnchor(footerScroll.bottomAnchor, constant = -10.0)

        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)
        c += sep.bottomAnchor.constraintEqualToAnchor(footerScroll.topAnchor)

        c += scrollView.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor)
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

    fun addEvent(info: UserEventInfo) {
        val wallMs = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
        events.add(0, Pair(info, wallMs))
        if (events.size > 200) events.removeAt(events.size - 1)
        CoroutineScope(Dispatchers.Main).launch { refresh() }
    }

    private fun triggerVideoPlayback() {
        if (isRecording) return
        isRecording = true
        videoBtn?.setTitle("Recording…", forState = UIControlStateNormal)
        videoBtn?.enabled = false
        val token = Kamper.startEvent("video_playback")
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2_000_000_000L), dispatch_get_main_queue()) {
            Kamper.endEvent(token)
            videoBtn?.setTitle("video_playback", forState = UIControlStateNormal)
            videoBtn?.enabled = true
            isRecording = false
        }
    }

    private fun handleLogCustom() {
        val raw = customInput.text?.trim().orEmpty()
        if (raw.isNotEmpty()) {
            Kamper.logEvent(raw)
            customInput.text = ""
        }
    }

    private fun clearEvents() {
        events.clear()
        refresh()
    }

    private fun refresh() {
        stackView.arrangedSubviews.filterIsInstance<UIView>().forEach { it.removeFromSuperview() }
        events.forEach { (event, wallMs) -> stackView.addArrangedSubview(eventRowView(event, wallMs)) }
        updateEmpty()
    }

    private fun updateEmpty() {
        emptyLabel.hidden = events.isNotEmpty()
        scrollView.hidden = events.isEmpty()
    }
}

private fun fmtTime(ms: Long): String {
    val sec = (ms / 1000) % 86400
    val h = sec / 3600; val m = (sec % 3600) / 60; val s = sec % 60
    return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
}

private fun eventRowView(event: UserEventInfo, wallMs: Long): UIView {
    val barColor = if (event.durationMs != null) Theme.uiColor(0x89B4FA) else Theme.uiColor(0xA6E3A1)

    val container = UIView().apply {
        backgroundColor = UIColor(red = 0.192, green = 0.196, blue = 0.267, alpha = 1.0)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val bar = UIView().apply {
        backgroundColor = barColor
        translatesAutoresizingMaskIntoConstraints = false
    }

    val nameLabel = UILabel().apply {
        text = event.name
        textColor = Theme.TEXT
        font = UIFont.monospacedSystemFontOfSize(13.0, weight = UIFontWeightMedium)
        translatesAutoresizingMaskIntoConstraints = false
    }

    val timeLabel = UILabel().apply {
        text = fmtTime(wallMs)
        textColor = Theme.MUTED
        font = UIFont.monospacedSystemFontOfSize(11.0, weight = 0.0)
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
        val durLabel = UILabel().apply {
            text = "${dur}ms"
            textColor = Theme.uiColor(0x89B4FA)
            font = UIFont.monospacedSystemFontOfSize(11.0, weight = 0.0)
            translatesAutoresizingMaskIntoConstraints = false
        }
        container.addSubview(durLabel)
        c += nameLabel.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 8.0)
        c += durLabel.leadingAnchor.constraintEqualToAnchor(bar.trailingAnchor, constant = 10.0)
        c += durLabel.topAnchor.constraintEqualToAnchor(nameLabel.bottomAnchor, constant = 2.0)
        c += durLabel.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -8.0)
    }
    if (event.durationMs == null) {
        c += nameLabel.bottomAnchor.constraintEqualToAnchor(container.bottomAnchor, constant = -8.0)
        c += nameLabel.topAnchor.constraintEqualToAnchor(container.topAnchor, constant = 8.0)
    }

    NSLayoutConstraint.activateConstraints(c)
    return container
}
