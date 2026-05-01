@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios.ui

import com.smellouk.kamper.memory.MemoryInfo
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*

class MemoryViewController : UIViewController(nibName = null, bundle = null) {
    private val heapRow = metricRow("Heap Used", Theme.GREEN)
    private val ramRow  = metricRow("RAM Used",  Theme.BLUE)

    private lateinit var heapDetail:  UILabel
    private lateinit var ramDetail:   UILabel
    private lateinit var lowMemLabel: UILabel
    private lateinit var allocButton: UIButton
    private lateinit var freeButton:  UIButton
    private lateinit var allocTarget: ActionTarget
    private lateinit var freeTarget:  ActionTarget

    private val allocations = mutableListOf<ByteArray>()

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        heapDetail  = detailLabel()
        ramDetail   = detailLabel()
        lowMemLabel = UILabel().apply {
            text  = ""
            font  = Theme.LABEL_FONT
            textColor = Theme.RED
            translatesAutoresizingMaskIntoConstraints = false
        }

        allocTarget = ActionTarget { allocations.add(ByteArray(32 * 1024 * 1024)) }
        freeTarget  = ActionTarget { allocations.clear() }
        allocButton = makeButton("Alloc 32 MB", allocTarget)
        freeButton  = makeButton("Free",        freeTarget)

        val heapLabel = sectionLabel("Heap Memory")
        val ramLabel  = sectionLabel("System RAM")
        val pssHint   = hintLabel("PSS metrics: Android only  ·  Simulator shows host Mac RAM")
        val sep       = makeSeparator()

        listOf(heapLabel, heapRow, heapDetail, ramLabel, ramRow, ramDetail, pssHint,
               sep, lowMemLabel, allocButton, freeButton).forEach { view.addSubview(it) }

        val pad = 20.0; val rh = METRIC_ROW_HEIGHT
        val c = mutableListOf<NSLayoutConstraint>()

        c += heapLabel.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = pad)
        c += heapLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += heapRow.topAnchor.constraintEqualToAnchor(heapLabel.bottomAnchor, constant = 6.0)
        c += heapRow.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += heapRow.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)
        c += heapRow.heightAnchor.constraintEqualToConstant(rh)

        c += heapDetail.topAnchor.constraintEqualToAnchor(heapRow.bottomAnchor, constant = 2.0)
        c += heapDetail.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += ramLabel.topAnchor.constraintEqualToAnchor(heapDetail.bottomAnchor, constant = 14.0)
        c += ramLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += ramRow.topAnchor.constraintEqualToAnchor(ramLabel.bottomAnchor, constant = 6.0)
        c += ramRow.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += ramRow.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)
        c += ramRow.heightAnchor.constraintEqualToConstant(rh)

        c += ramDetail.topAnchor.constraintEqualToAnchor(ramRow.bottomAnchor, constant = 2.0)
        c += ramDetail.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += pssHint.topAnchor.constraintEqualToAnchor(ramDetail.bottomAnchor, constant = 8.0)
        c += pssHint.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += sep.topAnchor.constraintEqualToAnchor(pssHint.bottomAnchor, constant = 12.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)

        c += allocButton.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 10.0)
        c += allocButton.trailingAnchor.constraintEqualToAnchor(freeButton.leadingAnchor, constant = -8.0)
        c += allocButton.centerYAnchor.constraintEqualToAnchor(freeButton.centerYAnchor)

        c += freeButton.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 10.0)
        c += freeButton.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += lowMemLabel.centerYAnchor.constraintEqualToAnchor(freeButton.centerYAnchor)
        c += lowMemLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

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
            heapDetail.text   = "${fmtMb1(alloc)} MB used  /  ${fmtMb1(max)} MB max"
        }
        with(info.ramInfo) {
            val total = totalRamInMb.toDouble()
            val avail = availableRamInMb.toDouble()
            val used  = total - avail
            val frac  = if (total > 0) used / total else 0.0
            ramRow.fraction  = frac
            ramRow.valueText = fmtPct(frac * 100)
            ramDetail.text   = "${fmtMb0(used)} MB used  /  ${fmtMb0(total)} MB total"
            lowMemLabel.text = if (isLowMemory) "⚠ Low Memory" else ""
        }
    }
}

private fun detailLabel() = UILabel().apply {
    text      = "—"
    font      = Theme.MONO_SMALL
    textColor = Theme.MUTED
    translatesAutoresizingMaskIntoConstraints = false
}

internal fun sectionLabel(text: String) = UILabel().apply {
    this.text  = text
    font       = Theme.LABEL_FONT
    textColor  = Theme.SUBTEXT
    translatesAutoresizingMaskIntoConstraints = false
}

internal fun hintLabel(text: String) = UILabel().apply {
    this.text  = text
    font       = Theme.HINT_FONT
    textColor  = Theme.MUTED
    translatesAutoresizingMaskIntoConstraints = false
}

internal fun makeSeparator() = UIView().apply {
    backgroundColor = Theme.SURFACE0
    translatesAutoresizingMaskIntoConstraints = false
}

internal fun fmtPct(v: Double): String { val i = v.toInt(); val d = ((v - i) * 10).toInt(); return "$i.$d%" }
internal fun fmtMb1(v: Double): String { val i = v.toInt(); val d = ((v - i) * 10).toInt(); return "$i.$d" }
internal fun fmtMb0(v: Double): String = v.toInt().toString()
