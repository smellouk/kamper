@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.samples.macos.ui

import com.smellouk.kamper.memory.MemoryInfo
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class MemoryView : NSView {

    private val heapRow    = metricRow("Heap Used", Theme.GREEN)
    private val ramRow     = metricRow("RAM Used",  Theme.BLUE)
    private val heapDetail = NSTextField.labelWithString("—").apply { font = Theme.MONO_SMALL; textColor = Theme.MUTED; translatesAutoresizingMaskIntoConstraints = false }
    private val ramDetail  = NSTextField.labelWithString("—").apply { font = Theme.MONO_SMALL; textColor = Theme.MUTED; translatesAutoresizingMaskIntoConstraints = false }
    private val lowMemLabel = NSTextField.labelWithString("").apply { font = Theme.LABEL_FONT; textColor = Theme.RED; translatesAutoresizingMaskIntoConstraints = false }

    private val allocations = mutableListOf<ByteArray>()
    private val allocTarget = ActionTarget { allocations.add(ByteArray(32 * 1024 * 1024)) }
    private val freeTarget  = ActionTarget { allocations.clear() }
    private val allocButton = makeButton("Alloc 32 MB", allocTarget)
    private val freeButton  = makeButton("Free", freeTarget)

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val heapLabel = sectionLbl("Heap Memory")
        val ramLabel  = sectionLbl("System RAM")
        val pssHint   = NSTextField.labelWithString("PSS metrics: Android only").apply { font = Theme.HINT_FONT; textColor = Theme.MUTED; translatesAutoresizingMaskIntoConstraints = false }
        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 1.0)).apply { boxType = NSBoxSeparator; translatesAutoresizingMaskIntoConstraints = false }

        listOf(heapLabel, heapRow, heapDetail, ramLabel, ramRow, ramDetail, pssHint, sep, lowMemLabel, allocButton, freeButton).forEach { addSubview(it) }

        val pad = 20.0
        val rh = METRIC_ROW_HEIGHT
        val c = mutableListOf<NSLayoutConstraint>()

        c += heapLabel.topAnchor.constraintEqualToAnchor(topAnchor, constant = pad)
        c += heapLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += heapRow.topAnchor.constraintEqualToAnchor(heapLabel.bottomAnchor, constant = 6.0)
        c += heapRow.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += heapRow.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += heapRow.heightAnchor.constraintEqualToConstant(rh)

        c += heapDetail.topAnchor.constraintEqualToAnchor(heapRow.bottomAnchor, constant = 2.0)
        c += heapDetail.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += ramLabel.topAnchor.constraintEqualToAnchor(heapDetail.bottomAnchor, constant = 14.0)
        c += ramLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += ramRow.topAnchor.constraintEqualToAnchor(ramLabel.bottomAnchor, constant = 6.0)
        c += ramRow.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
        c += ramRow.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += ramRow.heightAnchor.constraintEqualToConstant(rh)

        c += ramDetail.topAnchor.constraintEqualToAnchor(ramRow.bottomAnchor, constant = 2.0)
        c += ramDetail.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += pssHint.topAnchor.constraintEqualToAnchor(ramDetail.bottomAnchor, constant = 8.0)
        c += pssHint.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)
        c += sep.bottomAnchor.constraintEqualToAnchor(allocButton.topAnchor, constant = -10.0)

        c += lowMemLabel.centerYAnchor.constraintEqualToAnchor(allocButton.centerYAnchor)
        c += lowMemLabel.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)

        c += freeButton.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += freeButton.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += freeButton.heightAnchor.constraintEqualToConstant(28.0)

        c += allocButton.trailingAnchor.constraintEqualToAnchor(freeButton.leadingAnchor, constant = -8.0)
        c += allocButton.centerYAnchor.constraintEqualToAnchor(freeButton.centerYAnchor)
        c += allocButton.heightAnchor.constraintEqualToConstant(28.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: MemoryInfo) {
        if (info == MemoryInfo.INVALID) return
        with(info.heapMemoryInfo) {
            val alloc = allocatedInMb.toDouble()
            val max   = maxMemoryInMb.toDouble()
            val frac  = if (max > 0) alloc / max else 0.0
            heapRow.fraction  = frac
            heapRow.valueText = fmtPct(frac * 100)
            heapDetail.stringValue = "${fmtMb1(alloc)} MB used  /  ${fmtMb1(max)} MB max"
        }
        with(info.ramInfo) {
            val total = totalRamInMb.toDouble()
            val avail = availableRamInMb.toDouble()
            val used  = total - avail
            val frac  = if (total > 0) used / total else 0.0
            ramRow.fraction  = frac
            ramRow.valueText = fmtPct(frac * 100)
            ramDetail.stringValue = "${fmtMb0(used)} MB used  /  ${fmtMb0(total)} MB total"
            lowMemLabel.stringValue = if (isLowMemory) "⚠ Low Memory" else ""
        }
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}

private fun sectionLbl(text: String) = NSTextField.labelWithString(text).apply {
    font = Theme.LABEL_FONT; textColor = Theme.SUBTEXT; translatesAutoresizingMaskIntoConstraints = false
}

internal fun fmtPct(v: Double): String { val i = v.toInt(); val d = ((v - i) * 10).toInt(); return "$i.$d%" }
internal fun fmtMb1(v: Double): String { val i = v.toInt(); val d = ((v - i) * 10).toInt(); return "$i.$d" }
internal fun fmtMb0(v: Double): String = v.toInt().toString()
