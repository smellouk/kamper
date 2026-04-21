package com.smellouk.kamper.samples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import com.smellouk.kamper.samples.views.MetricRowView

class ThermalFragment : Fragment() {

    private var stateLabel: TextView? = null
    private var throttlingRow: MetricRowView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_thermal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        stateLabel = view.findViewById(R.id.thermalStateLabel)
        throttlingRow = view.findViewById<MetricRowView>(R.id.thermalThrottlingRow).also {
            it.label = "Throttling"
            it.barColor = 0xFFA6E3A1.toInt()
        }
    }

    override fun onDestroyView() {
        stateLabel = null; throttlingRow = null
        super.onDestroyView()
    }

    fun update(info: ThermalInfo) {
        activity?.runOnUiThread {
            stateLabel?.apply {
                text = info.state.name
                setTextColor(colorForState(info.state))
            }
            throttlingRow?.apply {
                fraction = if (info.isThrottling) 1f else 0f
                valueText = if (info.isThrottling) "YES" else "NO"
                barColor = if (info.isThrottling) 0xFFFAB387.toInt() else 0xFFA6E3A1.toInt()
            }
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
