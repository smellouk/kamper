@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.tvos.ui

import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

class MetricRowView : UIView {
    var label: String = ""
    var barColor: UIColor = Theme.TEXT
    var trackColor: UIColor = Theme.SURFACE0

    var fraction: Double = 0.0
        set(v) { field = v.coerceIn(0.0, 1.0); setNeedsDisplay() }

    var valueText: String = "0.0%"
        set(v) { field = v; setNeedsDisplay() }

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = UIColor.clearColor
        setOpaque(false)
    }

    override fun drawRect(rect: CValue<CGRect>) {
        super.drawRect(rect)
        val w = bounds.useContents { size.width }
        val h = bounds.useContents { size.height }

        val barX = LABEL_WIDTH
        val barW = w - LABEL_WIDTH - VALUE_WIDTH - BAR_H_PAD * 2
        val barY = (h - BAR_THICKNESS) / 2
        val arc  = BAR_THICKNESS / 2.0

        Theme.BASE.setFill()
        UIBezierPath.bezierPathWithRect(bounds).fill()

        val labelAttrs = mapOf<Any?, Any?>(
            NSFontAttributeName            to Theme.LABEL_FONT,
            NSForegroundColorAttributeName to Theme.TEXT
        )
        val labelNs = label as NSString
        val labelSz = labelNs.sizeWithAttributes(labelAttrs)
        val labelY  = (h - labelSz.useContents { height }) / 2
        labelNs.drawAtPoint(CGPointMake(0.0, labelY), withAttributes = labelAttrs)

        trackColor.setFill()
        UIBezierPath.bezierPathWithRoundedRect(
            CGRectMake(barX + BAR_H_PAD, barY, barW.coerceAtLeast(0.0), BAR_THICKNESS),
            cornerRadius = arc
        ).fill()

        val fillW = (barW * fraction).coerceAtLeast(if (fraction > 0.0) arc * 2 else 0.0)
        if (fillW > 0 && barW > 0) {
            barColor.setFill()
            UIBezierPath.bezierPathWithRoundedRect(
                CGRectMake(barX + BAR_H_PAD, barY, fillW, BAR_THICKNESS),
                cornerRadius = arc
            ).fill()
        }

        val valAttrs = mapOf<Any?, Any?>(
            NSFontAttributeName            to Theme.MONO_FONT,
            NSForegroundColorAttributeName to Theme.SUBTEXT
        )
        val valNs = valueText as NSString
        val valSz = valNs.sizeWithAttributes(valAttrs)
        val valX  = w - VALUE_WIDTH + (VALUE_WIDTH - valSz.useContents { width }) / 2
        val valY  = (h - valSz.useContents { height }) / 2
        valNs.drawAtPoint(CGPointMake(valX, valY), withAttributes = valAttrs)
    }
}

const val METRIC_ROW_HEIGHT = 80.0
private const val LABEL_WIDTH   = 160.0
private const val VALUE_WIDTH   = 120.0
private const val BAR_H_PAD     = 16.0
private const val BAR_THICKNESS = 24.0

fun metricRow(label: String, barColor: UIColor, trackColor: UIColor = Theme.SURFACE0): MetricRowView =
    MetricRowView(CGRectMake(0.0, 0.0, 0.0, METRIC_ROW_HEIGHT)).apply {
        this.label      = label
        this.barColor   = barColor
        this.trackColor = trackColor
    }
