@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import com.smellouk.kamper.fps.FpsInfo
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FpsView : NSView {

    private val fpsLabel = NSTextField.labelWithString("--").apply {
        font = platform.AppKit.NSFont.monospacedSystemFontOfSize(72.0, platform.AppKit.NSFontWeightBold)
        textColor = Theme.BLUE
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val unitLabel = NSTextField.labelWithString("fps").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val hintLabel = NSTextField.labelWithString("Timer-based frame measurement").apply {
        font = Theme.HINT_FONT
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val animView = AnimationView(NSMakeRect(0.0, 0.0, 0.0, 200.0))

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 1.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }
        listOf(fpsLabel, unitLabel, animView, sep, hintLabel).forEach { addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += fpsLabel.topAnchor.constraintEqualToAnchor(topAnchor, constant = 24.0)
        c += fpsLabel.centerXAnchor.constraintEqualToAnchor(centerXAnchor)
        c += fpsLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += fpsLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(fpsLabel.bottomAnchor, constant = 2.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(centerXAnchor)

        c += animView.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 16.0)
        c += animView.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += animView.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += animView.heightAnchor.constraintEqualToConstant(200.0)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)
        c += sep.topAnchor.constraintEqualToAnchor(animView.bottomAnchor, constant = 8.0)

        c += hintLabel.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 8.0)
        c += hintLabel.centerXAnchor.constraintEqualToAnchor(centerXAnchor)
        c += hintLabel.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -8.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: FpsInfo) {
        if (info == FpsInfo.INVALID) return
        fpsLabel.stringValue = info.fps.toString()
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}

class AnimationView : NSView {
    private var angle = 0.0
    private val timerTarget: NSObject

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val self = this
        timerTarget = object : NSObject() {
            @ObjCAction fun tick(timer: NSTimer?) {
                self.angle += 0.025
                self.needsDisplay = true
            }
        }
        NSTimer.scheduledTimerWithTimeInterval(
            0.016, target = timerTarget,
            selector = NSSelectorFromString("tick:"),
            userInfo = null, repeats = true
        )
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.MANTLE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()

        val w = bounds.useContents { size.width }
        val h = bounds.useContents { size.height }
        val cx = w / 2
        val cy = h / 2
        val r1 = min(w, h) * 0.30
        val r2 = min(w, h) * 0.15
        val n  = 6.0

        val palette = listOf(Theme.BLUE, Theme.GREEN, Theme.YELLOW, Theme.PEACH, Theme.MAUVE, Theme.TEAL)

        for (i in palette.indices) {
            val a = angle + i * kotlin.math.PI * 2 / n
            val x = cx + r1 * cos(a)
            val y = cy + r1 * sin(a)
            palette[i].setFill()
            NSBezierPath.bezierPathWithOvalInRect(NSMakeRect(x - 14, y - 14, 28.0, 28.0)).fill()
        }
        for (i in palette.indices) {
            val a = -angle * 1.5 + i * kotlin.math.PI * 2 / n
            val x = cx + r2 * cos(a)
            val y = cy + r2 * sin(a)
            val c = palette[(i + 3) % palette.size]
            NSColor.colorWithCalibratedRed(c.redComponent, green = c.greenComponent, blue = c.blueComponent, alpha = 0.7).setFill()
            NSBezierPath.bezierPathWithOvalInRect(NSMakeRect(x - 7, y - 7, 14.0, 14.0)).fill()
        }
        Theme.SURFACE1.setFill()
        NSBezierPath.bezierPathWithOvalInRect(NSMakeRect(cx - 5, cy - 5, 10.0, 10.0)).fill()
    }
}
