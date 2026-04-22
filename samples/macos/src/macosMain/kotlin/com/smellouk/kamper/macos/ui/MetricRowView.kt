@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class MetricRowView : NSView {

    var label: String = ""
    var barColor: NSColor = Theme.TEXT
    var trackColor: NSColor = Theme.SURFACE0

    var fraction: Double = 0.0
        set(v) { field = v.coerceIn(0.0, 1.0); needsDisplay = true }

    var valueText: String = "0.0%"
        set(v) { field = v; needsDisplay = true }

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        val w = bounds.useContents { size.width }
        val h = bounds.useContents { size.height }

        val barX = LABEL_WIDTH
        val barW = w - LABEL_WIDTH - VALUE_WIDTH - BAR_H_PAD * 2
        val barY = (h - BAR_THICKNESS) / 2
        val arc = BAR_THICKNESS / 2.0

        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()

        // Label
        val labelAttrs: Map<Any?, Any?> = mapOf(NSFontAttributeName to Theme.LABEL_FONT, NSForegroundColorAttributeName to Theme.TEXT)
        val labelSize = (label as NSString).sizeWithAttributes(labelAttrs)
        val labelY = (h - labelSize.useContents { height }) / 2
        (label as NSString).drawAtPoint(NSMakePoint(0.0, labelY), withAttributes = labelAttrs)

        // Track
        trackColor.setFill()
        NSBezierPath.bezierPathWithRoundedRect(
            NSMakeRect(barX + BAR_H_PAD, barY, barW.coerceAtLeast(0.0), BAR_THICKNESS),
            xRadius = arc, yRadius = arc
        ).fill()

        // Fill
        val fillW = (barW * fraction).coerceAtLeast(if (fraction > 0.0) arc * 2 else 0.0)
        if (fillW > 0 && barW > 0) {
            val fillPath = NSBezierPath.bezierPathWithRoundedRect(
                NSMakeRect(barX + BAR_H_PAD, barY, fillW, BAR_THICKNESS),
                xRadius = arc, yRadius = arc
            )
            val gradient = NSGradient(startingColor = barColor.lighter(0.25), endingColor = barColor)
            gradient?.drawInBezierPath(fillPath, angle = 0.0)
        }

        // Value (right-aligned in value column)
        val valAttrs: Map<Any?, Any?> = mapOf(NSFontAttributeName to Theme.MONO_FONT, NSForegroundColorAttributeName to Theme.SUBTEXT)
        val valSize = (valueText as NSString).sizeWithAttributes(valAttrs)
        val valX = w - VALUE_WIDTH + (VALUE_WIDTH - valSize.useContents { width }) / 2
        val valY = (h - valSize.useContents { height }) / 2
        (valueText as NSString).drawAtPoint(NSMakePoint(valX, valY), withAttributes = valAttrs)
    }

}

const val METRIC_ROW_HEIGHT = 38.0
private const val LABEL_WIDTH = 72.0
private const val VALUE_WIDTH  = 60.0
private const val BAR_H_PAD   = 8.0
private const val BAR_THICKNESS = 14.0

fun metricRow(label: String, barColor: NSColor, trackColor: NSColor = Theme.SURFACE0): MetricRowView =
    MetricRowView(NSMakeRect(0.0, 0.0, 0.0, METRIC_ROW_HEIGHT)).apply {
        this.label = label
        this.barColor = barColor
        this.trackColor = trackColor
    }

private fun NSColor.lighter(fraction: Double): NSColor =
    NSColor.colorWithCalibratedRed(
        redComponent + (1.0 - redComponent) * fraction,
        green = greenComponent + (1.0 - greenComponent) * fraction,
        blue  = blueComponent  + (1.0 - blueComponent)  * fraction,
        alpha = 1.0
    )
