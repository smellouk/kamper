@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.macos.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.gpu.GpuInfo
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class GpuView : NSView {

    // Header
    private val bigLabel = NSTextField.labelWithString("—").apply {
        font = NSFont.monospacedSystemFontOfSize(48.0, NSFontWeightBold)
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val unitLabel = NSTextField.labelWithString("GPU usage %").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }

    // Stat row keys
    private val memoryKey    = keyLabel("Memory")
    private val appUtilKey   = keyLabel("App Util")
    private val rendererKey  = keyLabel("Renderer Util")
    private val tilerKey     = keyLabel("Tiler Util")
    private val computeKey   = keyLabel("Compute Util")

    // Stat row values
    private val memoryValue   = valueLabel("—")
    private val appUtilValue  = valueLabel("N/A")
    private val rendererValue = valueLabel("N/A")
    private val tilerValue    = valueLabel("N/A")
    private val computeValue  = valueLabel("N/A")

    // Stress panel
    private val stressView = GpuStressView(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
        translatesAutoresizingMaskIntoConstraints = false
    }
    private var isStressing = false
    private val stressTarget = ActionTarget { toggleStress() }
    private val stressButton = makeButton("Stress GPU", stressTarget)

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val headerBg = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
            boxType = NSBoxCustom
            fillColor = Theme.MANTLE
            borderWidth = 0.0
            translatesAutoresizingMaskIntoConstraints = false
        }
        val headerSep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }
        val vDivider = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
            boxType = NSBoxCustom
            fillColor = Theme.SURFACE0
            borderWidth = 0.0
            translatesAutoresizingMaskIntoConstraints = false
        }
        val sep1 = rowSep()
        val sep2 = rowSep()
        val sep3 = rowSep()
        val sep4 = rowSep()

        listOf(
            headerBg,
            bigLabel, unitLabel, headerSep,
            memoryKey, memoryValue, sep1,
            appUtilKey, appUtilValue, sep2,
            rendererKey, rendererValue, sep3,
            tilerKey, tilerValue, sep4,
            computeKey, computeValue,
            vDivider, stressView, stressButton
        ).forEach { addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        // Header labels
        c += bigLabel.topAnchor.constraintEqualToAnchor(topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += unitLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += headerSep.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 20.0)
        c += headerSep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += headerSep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)

        // Header background covers top down to separator
        c += headerBg.topAnchor.constraintEqualToAnchor(topAnchor)
        c += headerBg.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += headerBg.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += headerBg.bottomAnchor.constraintEqualToAnchor(headerSep.topAnchor)

        // Vertical divider at center, spanning body
        c += vDivider.centerXAnchor.constraintEqualToAnchor(centerXAnchor)
        c += vDivider.widthAnchor.constraintEqualToConstant(1.0)
        c += vDivider.topAnchor.constraintEqualToAnchor(headerSep.bottomAnchor, constant = 8.0)
        c += vDivider.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -pad)

        // Left column stat rows
        addRow(c, memoryKey, memoryValue, sep1,
            topAnchor = headerSep.bottomAnchor, vDividerLeading = vDivider.leadingAnchor, pad = pad)
        addRow(c, appUtilKey, appUtilValue, sep2,
            topAnchor = sep1.bottomAnchor, vDividerLeading = vDivider.leadingAnchor, pad = pad)
        addRow(c, rendererKey, rendererValue, sep3,
            topAnchor = sep2.bottomAnchor, vDividerLeading = vDivider.leadingAnchor, pad = pad)
        addRow(c, tilerKey, tilerValue, sep4,
            topAnchor = sep3.bottomAnchor, vDividerLeading = vDivider.leadingAnchor, pad = pad)
        // Last row: no trailing separator
        c += computeKey.topAnchor.constraintEqualToAnchor(sep4.bottomAnchor, constant = 14.0)
        c += computeKey.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += computeKey.trailingAnchor.constraintLessThanOrEqualToAnchor(computeValue.leadingAnchor, constant = -8.0)
        c += computeValue.firstBaselineAnchor.constraintEqualToAnchor(computeKey.firstBaselineAnchor)
        c += computeValue.trailingAnchor.constraintEqualToAnchor(vDivider.leadingAnchor, constant = -pad)

        // Right column: stress canvas fills space above button
        c += stressView.topAnchor.constraintEqualToAnchor(headerSep.bottomAnchor, constant = pad)
        c += stressView.leadingAnchor.constraintEqualToAnchor(vDivider.trailingAnchor, constant = pad)
        c += stressView.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += stressView.bottomAnchor.constraintEqualToAnchor(stressButton.topAnchor, constant = -10.0)

        c += stressButton.leadingAnchor.constraintEqualToAnchor(vDivider.trailingAnchor, constant = pad)
        c += stressButton.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += stressButton.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += stressButton.heightAnchor.constraintEqualToConstant(28.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: GpuInfo) {
        if (info == GpuInfo.INVALID) return
        if (info == GpuInfo.UNSUPPORTED) {
            bigLabel.stringValue = "Unsupported"
            bigLabel.textColor = Theme.MUTED
            memoryValue.stringValue = "N/A"
            appUtilValue.stringValue = "N/A"
            rendererValue.stringValue = "N/A"
            tilerValue.stringValue = "N/A"
            computeValue.stringValue = "N/A"
            return
        }
        if (info.utilization < 0.0) {
            bigLabel.stringValue = "—%"
            bigLabel.textColor = Theme.MUTED
        } else {
            bigLabel.stringValue = "${NSString.stringWithFormat("%.1f", info.utilization)}%"
            bigLabel.textColor = Theme.MAUVE
        }
        memoryValue.stringValue = when {
            info.usedMemoryMb >= 0.0 && info.totalMemoryMb >= 0.0 ->
                "${info.usedMemoryMb.toInt()} MB / ${info.totalMemoryMb.toInt()} MB"
            info.totalMemoryMb >= 0.0 -> "— / ${info.totalMemoryMb.toInt()} MB"
            else -> "N/A"
        }
        appUtilValue.stringValue  = fmtUtil(info.appUtilization)
        rendererValue.stringValue = fmtUtil(info.rendererUtilization)
        tilerValue.stringValue    = fmtUtil(info.tilerUtilization)
        computeValue.stringValue  = fmtUtil(info.computeUtilization)
    }

    private fun fmtUtil(value: Double): String =
        if (value >= 0.0) "${NSString.stringWithFormat("%.1f", value)}%" else "N/A"

    private fun toggleStress() {
        isStressing = !isStressing
        if (isStressing) {
            Konitor.logEvent("gpu_stress_start")
            stressView.start()
            stressButton.title = "Stop Stress"
        } else {
            Konitor.logEvent("gpu_stress_stop")
            stressView.stop()
            stressButton.title = "Stress GPU"
        }
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }

    companion object {
        private fun keyLabel(text: String) = NSTextField.labelWithString(text).apply {
            font = Theme.LABEL_FONT
            textColor = Theme.MUTED
            translatesAutoresizingMaskIntoConstraints = false
        }

        private fun valueLabel(initial: String) = NSTextField.labelWithString(initial).apply {
            font = NSFont.monospacedSystemFontOfSize(13.0, NSFontWeightBold)
            textColor = Theme.TEXT
            alignment = NSTextAlignmentRight
            translatesAutoresizingMaskIntoConstraints = false
        }

        private fun rowSep() = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }

        private fun addRow(
            c: MutableList<NSLayoutConstraint>,
            key: NSTextField,
            value: NSTextField,
            sep: NSBox,
            topAnchor: NSLayoutYAxisAnchor,
            vDividerLeading: NSLayoutXAxisAnchor,
            pad: Double
        ) {
            c += key.topAnchor.constraintEqualToAnchor(topAnchor, constant = 14.0)
            c += key.leadingAnchor.constraintEqualToAnchor(key.superview!!.leadingAnchor, constant = pad)
            c += key.trailingAnchor.constraintLessThanOrEqualToAnchor(value.leadingAnchor, constant = -8.0)
            c += value.firstBaselineAnchor.constraintEqualToAnchor(key.firstBaselineAnchor)
            c += value.trailingAnchor.constraintEqualToAnchor(vDividerLeading, constant = -pad)
            c += sep.topAnchor.constraintEqualToAnchor(key.bottomAnchor, constant = 14.0)
            c += sep.leadingAnchor.constraintEqualToAnchor(key.superview!!.leadingAnchor)
            c += sep.trailingAnchor.constraintEqualToAnchor(vDividerLeading)
        }
    }
}
