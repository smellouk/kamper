@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import com.smellouk.kamper.gc.GcInfo
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class GcView : NSView {

    private val bigLabel = NSTextField.labelWithString("—").apply {
        font = NSFont.monospacedSystemFontOfSize(72.0, NSFontWeightBold)
        textColor = Theme.YELLOW
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val unitLabel = NSTextField.labelWithString("GC events / interval").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val pauseDeltaLabel = NSTextField.labelWithString("GC pause delta:  —").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.TEXT
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val totalCountLabel = NSTextField.labelWithString("Total GC count:  —").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.TEXT
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val simulateTarget = ActionTarget { simulateGc() }
    private val simulateButton = makeButton("Simulate GC", simulateTarget)

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 1.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }

        listOf(bigLabel, unitLabel, pauseDeltaLabel, totalCountLabel, sep, simulateButton).forEach { addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += bigLabel.topAnchor.constraintEqualToAnchor(topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(centerXAnchor)

        c += pauseDeltaLabel.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 24.0)
        c += pauseDeltaLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += pauseDeltaLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += totalCountLabel.topAnchor.constraintEqualToAnchor(pauseDeltaLabel.bottomAnchor, constant = 12.0)
        c += totalCountLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += totalCountLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)
        c += sep.bottomAnchor.constraintEqualToAnchor(simulateButton.topAnchor, constant = -10.0)

        c += simulateButton.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += simulateButton.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += simulateButton.heightAnchor.constraintEqualToConstant(28.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: GcInfo) {
        if (info == GcInfo.INVALID) return
        bigLabel.stringValue        = info.gcCountDelta.toString()
        pauseDeltaLabel.stringValue = "GC pause delta:  ${info.gcPauseMsDelta} ms"
        totalCountLabel.stringValue = "Total GC count:  ${info.gcCount}"
    }

    private fun simulateGc() {
        CoroutineScope(Dispatchers.Default).launch {
            repeat(200_000) { ByteArray(1024) }
        }
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}
