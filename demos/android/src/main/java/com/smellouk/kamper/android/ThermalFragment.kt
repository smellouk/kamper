package com.smellouk.kamper.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState

class ThermalFragment : Fragment() {

    private var stateLabel: TextView? = null
    private var throttlingValue: TextView? = null
    private var simulateButton: MaterialButton? = null

    @Volatile private var isStressing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_thermal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        stateLabel      = view.findViewById(R.id.thermalStateLabel)
        throttlingValue = view.findViewById(R.id.thermalThrottlingValue)
        simulateButton  = view.findViewById(R.id.thermalSimulateButton)
        simulateButton?.setOnClickListener { toggleStress() }
    }

    override fun onDestroyView() {
        isStressing = false
        stateLabel = null; throttlingValue = null; simulateButton = null
        super.onDestroyView()
    }

    fun update(info: ThermalInfo) {
        activity?.runOnUiThread {
            stateLabel?.apply {
                text = info.state.name
                setTextColor(colorForState(info.state))
            }
            throttlingValue?.apply {
                text = if (info.isThrottling) "YES" else "NO"
                setTextColor(
                    if (info.isThrottling) 0xFFFAB387.toInt() else 0xFFA6E3A1.toInt()
                )
            }
        }
    }

    private fun toggleStress() {
        isStressing = !isStressing
        if (isStressing) {
            simulateButton?.text = "Stop CPU Stress"
            val cores = Runtime.getRuntime().availableProcessors()
            repeat(cores) {
                Thread {
                    var x = 0.0
                    while (isStressing) { x = Math.sin(x + Math.PI) }
                }.apply { isDaemon = true; start() }
            }
        } else {
            simulateButton?.text = "Start CPU Stress"
        }
    }

    private fun colorForState(state: ThermalState): Int = when (state) {
        ThermalState.NONE                -> 0xFFA6E3A1.toInt()
        ThermalState.LIGHT               -> 0xFF94E2D5.toInt()
        ThermalState.MODERATE            -> 0xFFF9E2AF.toInt()
        ThermalState.SEVERE,
        ThermalState.CRITICAL,
        ThermalState.EMERGENCY,
        ThermalState.SHUTDOWN            -> 0xFFFAB387.toInt()
        ThermalState.UNKNOWN             -> 0xFFA6ADC8.toInt()
    }
}
