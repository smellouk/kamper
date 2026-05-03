@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.jank.JankInfo
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

class JankViewController : UIViewController(nibName = null, bundle = null) {
    private lateinit var bigLabel:      UILabel
    private lateinit var unitLabel:     UILabel
    private lateinit var ratioLabel:    UILabel
    private lateinit var worstLabel:    UILabel
    private lateinit var simulateTarget: ActionTarget

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        bigLabel = UILabel().apply {
            text          = "—"
            font          = UIFont.monospacedSystemFontOfSize(72.0, weight = 0.7)
            textColor     = Theme.MAUVE
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        unitLabel = UILabel().apply {
            text          = "dropped frames / window"
            font          = Theme.LABEL_FONT
            textColor     = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        ratioLabel = UILabel().apply {
            text      = "Janky ratio:  —"
            font      = Theme.LABEL_FONT
            textColor = Theme.TEXT
            translatesAutoresizingMaskIntoConstraints = false
        }
        worstLabel = UILabel().apply {
            text      = "Worst frame:  —"
            font      = Theme.LABEL_FONT
            textColor = Theme.TEXT
            translatesAutoresizingMaskIntoConstraints = false
        }

        val notSupportedHint = hintLabel("Jank detection not available on iOS")

        simulateTarget = ActionTarget { simulateJank() }
        val simulateBtn = makeButton("Simulate Jank", simulateTarget)

        val sep = makeSeparator()
        listOf(bigLabel, unitLabel, ratioLabel, worstLabel, notSupportedHint, sep, simulateBtn).forEach { view.addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += bigLabel.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor)

        c += ratioLabel.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 24.0)
        c += ratioLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += ratioLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += worstLabel.topAnchor.constraintEqualToAnchor(ratioLabel.bottomAnchor, constant = 12.0)
        c += worstLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += worstLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += notSupportedHint.topAnchor.constraintEqualToAnchor(worstLabel.bottomAnchor, constant = 12.0)
        c += notSupportedHint.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += sep.topAnchor.constraintEqualToAnchor(notSupportedHint.bottomAnchor, constant = 12.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)

        c += simulateBtn.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 12.0)
        c += simulateBtn.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: JankInfo) {
        if (info == JankInfo.INVALID) return
        if (info == JankInfo.UNSUPPORTED) {
            bigLabel.text      = "N/A"
            bigLabel.textColor = Theme.MUTED
            ratioLabel.text    = "Janky ratio:  —"
            worstLabel.text    = "Worst frame:  —"
            return
        }
        bigLabel.textColor = Theme.MAUVE
        bigLabel.text   = info.droppedFrames.toString()
        val ratio = info.jankyFrameRatio * 100.0
        val ri = ratio.toInt(); val rd = ((ratio - ri) * 10).toInt()
        ratioLabel.text = "Janky ratio:  $ri.$rd%"
        worstLabel.text = "Worst frame:  ${info.worstFrameMs} ms"
    }

    private fun simulateJank() {
        Kamper.logEvent("jank_simulate")
        val end = NSDate.dateWithTimeIntervalSinceNow(0.2)
        while (NSDate.date().compare(end) == NSOrderedAscending) {}
    }
}
