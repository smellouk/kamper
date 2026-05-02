@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*
import platform.darwin.NSObject
import kotlin.random.Random

private const val BALL_COUNT = 120
private const val MAX_RADIUS = 55.0
private const val CORNER_RADIUS = 8.0

class GpuStressView : NSView {

    private var timer: NSTimer? = null
    private var timerTarget: NSObject? = null
    private var active = false

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        wantsLayer = true
    }

    fun start() {
        active = true
        val self = this
        timerTarget = object : NSObject() {
            @ObjCAction fun tick(sender: NSTimer?) {
                self.needsDisplay = true
            }
        }
        timer = NSTimer.scheduledTimerWithTimeInterval(
            1.0 / 60.0, target = timerTarget!!,
            selector = NSSelectorFromString("tick:"),
            userInfo = null, repeats = true
        )
    }

    fun stop() {
        active = false
        timer?.invalidate()
        timer = null
        timerTarget = null
        needsDisplay = true
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        val roundedPath = NSBezierPath.bezierPathWithRoundedRect(bounds, xRadius = CORNER_RADIUS, yRadius = CORNER_RADIUS)
        roundedPath.addClip()

        Theme.MANTLE.setFill()
        roundedPath.fill()

        if (!active) return

        val w = bounds.useContents { size.width }
        val h = bounds.useContents { size.height }

        repeat(BALL_COUNT) {
            val cx = Random.nextDouble() * w
            val cy = Random.nextDouble() * h
            val r  = Random.nextDouble() * MAX_RADIUS + 8.0

            val hue = Random.nextDouble()
            val inner = NSColor.colorWithCalibratedHue(hue, saturation = 1.0, brightness = 1.0, alpha = 0.9)
            val outer  = NSColor.colorWithCalibratedHue(hue, saturation = 0.6, brightness = 0.4, alpha = 0.0)

            val gradient = NSGradient(startingColor = inner, endingColor = outer)
            val path = NSBezierPath.bezierPathWithOvalInRect(NSMakeRect(cx - r, cy - r, r * 2, r * 2))
            gradient?.drawInBezierPath(path, relativeCenterPosition = NSMakePoint(0.0, 0.0))
        }
    }

    override fun isOpaque(): Boolean = false
}
