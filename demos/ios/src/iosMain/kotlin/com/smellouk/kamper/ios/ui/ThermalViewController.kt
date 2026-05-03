@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.kamper.ios.ui

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

class ThermalViewController : UIViewController(nibName = null, bundle = null) {
    private lateinit var bigLabel:        UILabel
    private lateinit var unitLabel:       UILabel
    private lateinit var temperatureLabel: UILabel
    private lateinit var throttlingLabel:  UILabel
    private lateinit var simHintLabel:    UILabel
    private lateinit var stressTarget:   ActionTarget
    private lateinit var stressBtn:      UIButton
    private var stressJobs = listOf<Job>()
    private var stressActive = false

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.BASE

        bigLabel = UILabel().apply {
            text          = "N/A"
            font          = UIFont.monospacedSystemFontOfSize(48.0, weight = 0.7)
            textColor     = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        unitLabel = UILabel().apply {
            text          = "thermal state"
            font          = Theme.LABEL_FONT
            textColor     = Theme.MUTED
            textAlignment = NSTextAlignmentCenter
            translatesAutoresizingMaskIntoConstraints = false
        }
        temperatureLabel = UILabel().apply {
            text      = "Temperature:  —"
            font      = Theme.LABEL_FONT
            textColor = Theme.TEXT
            translatesAutoresizingMaskIntoConstraints = false
        }
        throttlingLabel = UILabel().apply {
            text      = "Throttling:   —"
            font      = Theme.LABEL_FONT
            textColor = Theme.TEXT
            translatesAutoresizingMaskIntoConstraints = false
        }

        simHintLabel = hintLabel("Not available on simulator")

        stressTarget = ActionTarget { toggleStress() }
        stressBtn    = makeButton("Start CPU Stress", stressTarget)

        val sep = makeSeparator()
        listOf(bigLabel, unitLabel, temperatureLabel, throttlingLabel, simHintLabel, sep, stressBtn).forEach { view.addSubview(it) }

        val pad = 20.0
        val c = mutableListOf<NSLayoutConstraint>()

        c += bigLabel.topAnchor.constraintEqualToAnchor(view.safeAreaLayoutGuide.topAnchor, constant = 24.0)
        c += bigLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += bigLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += unitLabel.topAnchor.constraintEqualToAnchor(bigLabel.bottomAnchor, constant = 4.0)
        c += unitLabel.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor)

        c += temperatureLabel.topAnchor.constraintEqualToAnchor(unitLabel.bottomAnchor, constant = 24.0)
        c += temperatureLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += temperatureLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += throttlingLabel.topAnchor.constraintEqualToAnchor(temperatureLabel.bottomAnchor, constant = 8.0)
        c += throttlingLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)
        c += throttlingLabel.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        c += simHintLabel.topAnchor.constraintEqualToAnchor(throttlingLabel.bottomAnchor, constant = 12.0)
        c += simHintLabel.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor, constant = pad)

        c += sep.topAnchor.constraintEqualToAnchor(simHintLabel.bottomAnchor, constant = 12.0)
        c += sep.leadingAnchor.constraintEqualToAnchor(view.leadingAnchor)
        c += sep.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor)
        c += sep.heightAnchor.constraintEqualToConstant(1.0)

        c += stressBtn.topAnchor.constraintEqualToAnchor(sep.bottomAnchor, constant = 12.0)
        c += stressBtn.trailingAnchor.constraintEqualToAnchor(view.trailingAnchor, constant = -pad)

        NSLayoutConstraint.activateConstraints(c)
    }

    fun update(info: ThermalInfo) {
        if (info == ThermalInfo.INVALID) return
        bigLabel.text           = info.state.name
        bigLabel.textColor      = stateColor(info.state)
        temperatureLabel.text   = "Temperature:  ${temperatureString(info)}"
        throttlingLabel.text    = "Throttling:   ${if (info.isThrottling) "YES" else "NO"}"
    }

    private fun temperatureString(info: ThermalInfo): String {
        if (info.temperatureC >= 0) {
            val t = (info.temperatureC * 10).toLong()
            return "${t / 10}.${t % 10} °C"
        }
        return when (info.state) {
            ThermalState.NONE     -> "< 60 °C"
            ThermalState.LIGHT    -> "60 – 75 °C"
            ThermalState.MODERATE -> "75 – 85 °C"
            ThermalState.SEVERE   -> "85 – 95 °C"
            ThermalState.CRITICAL,
            ThermalState.EMERGENCY,
            ThermalState.SHUTDOWN -> "> 95 °C"
            else                  -> "—"
        }
    }

    private fun stateColor(state: ThermalState): UIColor = when (state) {
        ThermalState.NONE, ThermalState.LIGHT -> Theme.GREEN
        ThermalState.MODERATE                 -> Theme.YELLOW
        ThermalState.SEVERE,
        ThermalState.CRITICAL,
        ThermalState.EMERGENCY,
        ThermalState.SHUTDOWN                 -> Theme.PEACH
        ThermalState.UNKNOWN,
        ThermalState.UNSUPPORTED              -> Theme.MUTED
    }

    private fun toggleStress() {
        if (!stressActive) {
            Kamper.logEvent("thermal_stress_start")
            stressActive = true
            stressBtn.setTitle("Stop CPU Stress", forState = UIControlStateNormal)
            stressJobs = (0 until 4).map {
                CoroutineScope(Dispatchers.Default).launch {
                    var x = 0L
                    while (isActive) { x = x * 6_364_136_223_846_793_005L + 1_442_695_040_888_963_407L }
                }
            }
        } else {
            Kamper.logEvent("thermal_stress_stop")
            stressActive = false
            stressBtn.setTitle("Start CPU Stress", forState = UIControlStateNormal)
            stressJobs.forEach { it.cancel() }
            stressJobs = emptyList()
        }
    }
}
