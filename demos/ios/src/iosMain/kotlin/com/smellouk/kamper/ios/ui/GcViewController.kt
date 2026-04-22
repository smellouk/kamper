@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios.ui

import com.smellouk.kamper.gc.GcInfo
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

class GcViewController : UIViewController(nibName = null, bundle = null) {
    private lateinit var bigLabel:       UILabel
    private lateinit var unitLabel:      UILabel
    private lateinit var pauseDeltaLabel: UILabel
    private lateinit var totalCountLabel: UILabel
    private lateinit var simulateTarget: ActionTarget

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        bigLabel = UILabel().apply {
            text          = "—"
            font          = UIFont.monospacedSystemFontOfSize(72.0, weight = 0.7)
            textColor     = Theme.YELLOW
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        unitLabel = UILabel().apply {
            text          = "GC events / interval"
            font          = Theme.LABEL_FONT
            textColor     = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        pauseDeltaLabel = UILabel().apply {
            text      = "GC pause delta:  —"
            font      = Theme.LABEL_FONT
            textColor = Theme.TEXT
            translatesAutoresizingMaskIntoConstraints = false
        }
        totalCountLabel = UILabel().apply {
            text      = "Total GC count:  —"
            font      = Theme.LABEL_FONT
            textColor = Theme.TEXT
            translatesAutoresizingMaskIntoConstraints = false
        }

        simulateTarget = ActionTarget { simulateGc() }
        val simulateBtn = makeButton("Simulate GC", simulateTarget)

        val sep = makeSeparator()
        listOf(bigLabel, unitLabel, pauseDeltaLabel, totalCountLabel, sep, simulateBtn).forEach { view.addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += bigLabel.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor)

        c += pauseDeltaLabel.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 24.0)
        c += pauseDeltaLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += pauseDeltaLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += totalCountLabel.topAnchor.constraintEqualToAnchor(pauseDeltaLabel.bottomAnchor, constant = 12.0)
        c += totalCountLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += totalCountLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += sep.topAnchor.constraintEqualToAnchor(totalCountLabel.bottomAnchor, constant = 24.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)

        c += simulateBtn.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 12.0)
        c += simulateBtn.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: GcInfo) {
        if (info == GcInfo.INVALID) return
        bigLabel.text        = info.gcCountDelta.toString()
        pauseDeltaLabel.text = "GC pause delta:  ${info.gcPauseMsDelta} ms"
        totalCountLabel.text = "Total GC count:  ${info.gcCount}"
    }

    private fun simulateGc() {
        CoroutineScope(Dispatchers.Default).launch {
            repeat(200_000) { ByteArray(1024) }
        }
    }
}
