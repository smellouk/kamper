@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.ios.ui

import com.smellouk.konitor.gpu.GpuInfo
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

class GpuViewController : UIViewController(nibName = null, bundle = null) {
    private lateinit var bigLabel:    UILabel
    private lateinit var unitLabel:   UILabel
    private lateinit var memoryLabel: UILabel

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        bigLabel = UILabel().apply {
            text          = "—"
            font          = UIFont.monospacedSystemFontOfSize(72.0, weight = 0.7)
            textColor     = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        unitLabel = UILabel().apply {
            text          = "GPU usage %"
            font          = Theme.LABEL_FONT
            textColor     = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        memoryLabel = UILabel().apply {
            text      = "Memory:  —"
            font      = Theme.LABEL_FONT
            textColor = Theme.TEXT
            translatesAutoresizingMaskIntoConstraints = false
        }

        val restrictedHint = hintLabel("GPU access restricted on iOS — see ADR-007")

        listOf(bigLabel, unitLabel, memoryLabel, restrictedHint).forEach { view.addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += bigLabel.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor)

        c += memoryLabel.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 24.0)
        c += memoryLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += memoryLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += restrictedHint.topAnchor.constraintEqualToAnchor(memoryLabel.bottomAnchor, constant = 12.0)
        c += restrictedHint.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: GpuInfo) {
        if (info == GpuInfo.INVALID) return
        if (info == GpuInfo.UNSUPPORTED) {
            bigLabel.text       = "N/A"
            bigLabel.textColor  = Theme.MUTED
            memoryLabel.text    = "Memory:  —"
            return
        }
        if (info.utilization < 0.0) {
            bigLabel.text      = "—%"
            bigLabel.textColor = Theme.MUTED
        } else {
            bigLabel.text      = "${fmtPct(info.utilization)}"
            bigLabel.textColor = Theme.MAUVE
        }
        memoryLabel.text = when {
            info.usedMemoryMb >= 0.0 && info.totalMemoryMb >= 0.0 ->
                "Memory:  ${info.usedMemoryMb.toInt()} MB / ${info.totalMemoryMb.toInt()} MB"
            info.totalMemoryMb >= 0.0 ->
                "Memory:  — / ${info.totalMemoryMb.toInt()} MB"
            else -> "Memory:  N/A"
        }
    }
}
