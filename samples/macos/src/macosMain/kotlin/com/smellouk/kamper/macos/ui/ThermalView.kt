@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.macos.ui

import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class ThermalView : NSView {

    private val bigLabel = NSTextField.labelWithString("UNKNOWN").apply {
        font = NSFont.monospacedSystemFontOfSize(48.0, NSFontWeightBold)
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val unitLabel = NSTextField.labelWithString("thermal state").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.MUTED
        alignment = NSTextAlignmentCenter
        translatesAutoresizingMaskIntoConstraints = false
    }
    private val throttlingLabel = NSTextField.labelWithString("Throttling:  —").apply {
        font = Theme.LABEL_FONT
        textColor = Theme.TEXT
        translatesAutoresizingMaskIntoConstraints = false
    }
    private var stressJobs = listOf<Job>()
    private var stressActive = false
    private val stressTarget = ActionTarget { toggleStress() }
    private val stressButton = makeButton("Start CPU Stress", stressTarget)

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 1.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }

        listOf(bigLabel, unitLabel, throttlingLabel, sep, stressButton).forEach { addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += bigLabel.topAnchor.constraintEqualToAnchor(topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(centerXAnchor)

        c += throttlingLabel.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 24.0)
        c += throttlingLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += throttlingLabel.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)
        c += sep.bottomAnchor.constraintEqualToAnchor(stressButton.topAnchor, constant = -10.0)

        c += stressButton.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += stressButton.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += stressButton.heightAnchor.constraintEqualToConstant(28.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: ThermalInfo) {
        if (info == ThermalInfo.INVALID) return
        bigLabel.stringValue       = info.state.name
        bigLabel.textColor         = stateColor(info.state)
        throttlingLabel.stringValue = "Throttling:  ${if (info.isThrottling) "YES" else "NO"}"
    }

    private fun stateColor(state: ThermalState): NSColor = when (state) {
        ThermalState.NONE, ThermalState.LIGHT -> Theme.GREEN
        ThermalState.MODERATE                 -> Theme.YELLOW
        ThermalState.SEVERE,
        ThermalState.CRITICAL,
        ThermalState.EMERGENCY,
        ThermalState.SHUTDOWN                 -> Theme.PEACH
        ThermalState.UNKNOWN                  -> Theme.MUTED
    }

    private fun toggleStress() {
        if (!stressActive) {
            stressActive = true
            stressButton.title = "Stop CPU Stress"
            stressJobs = (0 until 4).map {
                CoroutineScope(Dispatchers.Default).launch {
                    var x = 0L
                    while (isActive) { x = x * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L }
                }
            }
        } else {
            stressActive = false
            stressButton.title = "Start CPU Stress"
            stressJobs.forEach { it.cancel() }
            stressJobs = emptyList()
        }
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}
