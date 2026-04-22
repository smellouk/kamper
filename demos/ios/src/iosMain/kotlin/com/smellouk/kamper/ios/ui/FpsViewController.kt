@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios.ui

import com.smellouk.kamper.fps.FpsInfo
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class FpsViewController : UIViewController(nibName = null, bundle = null) {
    private lateinit var fpsLabel:  UILabel
    private lateinit var unitLabel: UILabel
    private lateinit var hintLabel: UILabel
    private lateinit var animView:  AnimationView

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        fpsLabel = UILabel().apply {
            text      = "--"
            font      = UIFont.monospacedSystemFontOfSize(72.0, weight = 0.7)  // bold
            textColor = Theme.BLUE
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        unitLabel = UILabel().apply {
            text      = "fps"
            font      = Theme.LABEL_FONT
            textColor = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        hintLabel = UILabel().apply {
            text      = "Timer-based frame measurement"
            font      = Theme.HINT_FONT
            textColor = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        animView = AnimationView(CGRectMake(0.0, 0.0, 0.0, 200.0))

        val sep = makeSeparator()
        listOf(fpsLabel, unitLabel, animView, sep, hintLabel).forEach { view.addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()
        c += fpsLabel.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = 24.0)
        c += fpsLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += fpsLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(fpsLabel.bottomAnchor, constant = 2.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor)

        c += animView.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 16.0)
        c += animView.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += animView.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += animView.heightAnchor.constraintEqualToConstant(200.0)

        c += sep.topAnchor.constraintEqualToAnchor(animView.bottomAnchor, constant = 8.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)

        c += hintLabel.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 8.0)
        c += hintLabel.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: FpsInfo) {
        if (info == FpsInfo.INVALID) return
        fpsLabel.text = info.fps.toString()
    }
}

class AnimationView : UIView {
    private var angle = 0.0
    private val timerTarget: NSObject

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = Theme.MANTLE
        setOpaque(true)

        val self = this
        timerTarget = object : NSObject() {
            @ObjCAction fun tick(timer: NSTimer?) {
                self.angle += 0.025
                self.setNeedsDisplay()
            }
        }
        NSTimer.scheduledTimerWithTimeInterval(
            0.016, target = timerTarget,
            selector  = NSSelectorFromString("tick:"),
            userInfo  = null,
            repeats   = true
        )
    }

    override fun drawRect(rect: CValue<CGRect>) {
        super.drawRect(rect)
        Theme.MANTLE.setFill()
        UIBezierPath.bezierPathWithRect(bounds).fill()

        val w  = bounds.useContents { size.width }
        val h  = bounds.useContents { size.height }
        val cx = w / 2; val cy = h / 2
        val r1 = min(w, h) * 0.30
        val r2 = min(w, h) * 0.15
        val n  = 6.0

        val palette = listOf(Theme.BLUE, Theme.GREEN, Theme.YELLOW, Theme.PEACH, Theme.MAUVE, Theme.TEAL)

        for (i in palette.indices) {
            val a = angle + i * kotlin.math.PI * 2 / n
            val x = cx + r1 * cos(a); val y = cy + r1 * sin(a)
            palette[i].setFill()
            UIBezierPath.bezierPathWithOvalInRect(CGRectMake(x - 14, y - 14, 28.0, 28.0)).fill()
        }
        for (i in palette.indices) {
            val a = -angle * 1.5 + i * kotlin.math.PI * 2 / n
            val x = cx + r2 * cos(a); val y = cy + r2 * sin(a)
            val c = palette[(i + 3) % palette.size]
            c.colorWithAlphaComponent(0.7).setFill()
            UIBezierPath.bezierPathWithOvalInRect(CGRectMake(x - 7, y - 7, 14.0, 14.0)).fill()
        }
        Theme.SURFACE1.setFill()
        UIBezierPath.bezierPathWithOvalInRect(CGRectMake(cx - 5, cy - 5, 10.0, 10.0)).fill()
    }
}
