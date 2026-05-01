@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.tvos.ui

import com.smellouk.kamper.cpu.CpuInfo
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.Foundation.*
import platform.UIKit.*

class CpuViewController : UIViewController(nibName = null, bundle = null) {
    private val totalRow  = metricRow("Total",   Theme.BLUE)
    private val appRow    = metricRow("App",     Theme.GREEN)
    private val userRow   = metricRow("User",    Theme.YELLOW)
    private val systemRow = metricRow("System",  Theme.PEACH)
    private val ioRow     = metricRow("IO Wait", Theme.MAUVE)

    private var loadJobs   = listOf<Job>()
    private var loadActive = false
    private lateinit var loadButton: UIButton
    private lateinit var loadTarget: ActionTarget

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        loadTarget = ActionTarget { toggleLoad() }
        loadButton = makeButton("Start CPU Load", loadTarget)

        val rows = listOf(totalRow, appRow, userRow, systemRow, ioRow)
        rows.forEach { view.addSubview(it) }

        val sep = makeSeparator()
        view.addSubview(sep)
        view.addSubview(loadButton)

        val pad = 80.0; val gap = 12.0; val rh = METRIC_ROW_HEIGHT
        val c = mutableListOf<NSLayoutConstraint>()
        var prevBottom: NSLayoutYAxisAnchor = view.safeAreaLayoutGuide.topAnchor

        rows.forEachIndexed { i, row ->
            c += row.topAnchor.constraintEqualToAnchor(prevBottom, constant = if (i == 0) pad else gap)
            c += row.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
            c += row.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)
            c += row.heightAnchor.constraintEqualToConstant(rh)
            prevBottom = row.bottomAnchor
        }
        c += sep.topAnchor.constraintEqualToAnchor(prevBottom, constant = 24.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(2.0)

        c += loadButton.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 20.0)
        c += loadButton.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

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
            loadActive = true
            loadButton.setTitle("Stop CPU Load", forState = UIControlStateNormal)
            loadJobs = (0 until 4).map {
                CoroutineScope(Dispatchers.Default).launch {
                    var x = 0L
                    while (isActive) { x = x * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L }
                }
            }
        } else {
            loadActive = false
            loadButton.setTitle("Start CPU Load", forState = UIControlStateNormal)
            loadJobs.forEach { it.cancel() }
            loadJobs = emptyList()
        }
    }
}
