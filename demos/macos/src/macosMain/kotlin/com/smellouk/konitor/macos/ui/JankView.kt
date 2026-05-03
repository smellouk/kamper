@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.macos.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.jank.JankInfo
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class JankView : NSView {

    private val bigLabel = NSTextField.labelWithString("—").apply {
        font = NSFont.monospacedSystemFontOfSize(72.0, NSFontWeightBold)
        textColor = Theme.MAUVE
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val unitLabel = NSTextField.labelWithString("dropped frames / window").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val ratioLabel = NSTextField.labelWithString("Janky ratio:  —").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.TEXT
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val worstLabel = NSTextField.labelWithString("Worst frame:  —").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.TEXT
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val simulateTarget = ActionTarget { simulateJank() }
    private val simulateButton = makeButton("Simulate Jank", simulateTarget)

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }

        listOf(bigLabel, unitLabel, ratioLabel, worstLabel, sep, simulateButton).forEach { addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += bigLabel.topAnchor.constraintEqualToAnchor(topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(centerXAnchor)

        c += ratioLabel.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 24.0)
        c += ratioLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += ratioLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += worstLabel.topAnchor.constraintEqualToAnchor(ratioLabel.bottomAnchor, constant = 12.0)
        c += worstLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += worstLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.bottomAnchor.constraintEqualToAnchor(simulateButton.topAnchor, constant = -10.0)

        c += simulateButton.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += simulateButton.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += simulateButton.heightAnchor.constraintEqualToConstant(28.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: JankInfo) {
        if (info == JankInfo.INVALID) return
        if (info == JankInfo.UNSUPPORTED) {
            bigLabel.stringValue    = "—"
            unitLabel.stringValue   = "Not supported on macOS"
            ratioLabel.stringValue  = "Janky ratio:  N/A"
            worstLabel.stringValue  = "Worst frame:  N/A"
            simulateButton.setEnabled(false)
            return
        }
        bigLabel.stringValue   = info.droppedFrames.toString()
        val ratio = info.jankyFrameRatio * 100.0
        val ri = ratio.toInt(); val rd = ((ratio - ri) * 10).toInt()
        ratioLabel.stringValue = "Janky ratio:  $ri.$rd%"
        worstLabel.stringValue = "Worst frame:  ${info.worstFrameMs} ms"
    }

    private fun simulateJank() {
        Konitor.logEvent("jank_simulate")
        val end = NSDate.dateWithTimeIntervalSinceNow(0.2)
        while (NSDate.date().compare(end) == NSOrderedAscending) {}
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}
