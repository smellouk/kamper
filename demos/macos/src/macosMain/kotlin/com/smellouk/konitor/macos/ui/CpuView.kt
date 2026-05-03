@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.macos.ui

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.cpu.CpuInfo
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AppKit.*
import platform.CoreGraphics.CGRect
import platform.Foundation.*

class CpuView : NSView {

    private val totalRow  = metricRow("Total",   Theme.BLUE)
    private val appRow    = metricRow("App",     Theme.GREEN)
    private val userRow   = metricRow("User",    Theme.YELLOW)
    private val systemRow = metricRow("System",  Theme.PEACH)
    private val ioRow     = metricRow("IO Wait", Theme.MAUVE)

    private var loadJobs = listOf<Job>()
    private var loadActive = false
    private val loadTarget = ActionTarget { toggleLoad() }
    private val loadButton = makeButton("Start CPU Load", loadTarget)

    @OverrideInit constructor(frame: CValue<CGRect>) : super(frame) {
        translatesAutoresizingMaskIntoConstraints = false
        wantsLayer = true

        val rows = listOf(totalRow, appRow, userRow, systemRow, ioRow)
        rows.forEach { addSubview(it) }

        val sep = NSBox(NSMakeRect(0.0, 0.0, 0.0, 0.0)).apply {
            boxType = NSBoxSeparator
            translatesAutoresizingMaskIntoConstraints = false
        }
        addSubview(sep)
        addSubview(loadButton)

        val pad = 20.0
        val gap = 6.0
        val rh = METRIC_ROW_HEIGHT

        var prev = topAnchor
        val c = mutableListOf<NSLayoutConstraint>()
        rows.forEachIndexed { i, row ->
            c += row.topAnchor.constraintEqualToAnchor(prev, constant = if (i == 0) pad else gap)
            c += row.leadingAnchor.constraintEqualToAnchor(leadingAnchor, constant = pad)
            c += row.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
            c += row.heightAnchor.constraintEqualToConstant(rh)
            prev = row.bottomAnchor
        }

        c += sep.leadingAnchor.constraintEqualToAnchor(leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(trailingAnchor)
        c += sep.bottomAnchor.constraintEqualToAnchor(loadButton.topAnchor, constant = -10.0)

        c += loadButton.trailingAnchor.constraintEqualToAnchor(trailingAnchor, constant = -pad)
        c += loadButton.bottomAnchor.constraintEqualToAnchor(bottomAnchor, constant = -10.0)
        c += loadButton.heightAnchor.constraintEqualToConstant(28.0)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: CpuInfo) {
        if (info == CpuInfo.INVALID) return
        totalRow.fraction  = info.totalUseRatio;  totalRow.valueText  = fmtPct(info.totalUseRatio * 100)
        appRow.fraction    = info.appRatio;        appRow.valueText    = fmtPct(info.appRatio * 100)
        userRow.fraction   = info.userRatio;       userRow.valueText   = fmtPct(info.userRatio * 100)
        systemRow.fraction = info.systemRatio;     systemRow.valueText = fmtPct(info.systemRatio * 100)
        ioRow.fraction     = info.ioWaitRatio;     ioRow.valueText     = fmtPct(info.ioWaitRatio * 100)
    }

    private fun toggleLoad() {
        if (!loadActive) {
            Konitor.logEvent("cpu_load_start")
            loadActive = true
            loadButton.title = "Stop CPU Load"
            loadJobs = (0 until 4).map {
                CoroutineScope(Dispatchers.Default).launch {
                    var x = 0L
                    while (isActive) { x = x * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L }
                }
            }
        } else {
            Konitor.logEvent("cpu_load_stop")
            loadActive = false
            loadButton.title = "Start CPU Load"
            loadJobs.forEach { it.cancel() }
            loadJobs = emptyList()
        }
    }

    override fun drawRect(dirtyRect: CValue<CGRect>) {
        super.drawRect(dirtyRect)
        Theme.BASE.setFill()
        NSBezierPath.bezierPathWithRect(bounds).fill()
    }
}
