package com.smellouk.kamper.samples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.samples.views.MetricRowView

class JankFragment : Fragment() {

    private var droppedLabel: TextView? = null
    private var ratioRow: MetricRowView? = null
    private var worstRow: MetricRowView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_jank, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        droppedLabel = view.findViewById(R.id.jankDroppedLabel)
        ratioRow = view.findViewById<MetricRowView>(R.id.jankRatioRow).also {
            it.label = "Janky ratio"
            it.barColor = 0xFFFAB387.toInt()
        }
        worstRow = view.findViewById<MetricRowView>(R.id.jankWorstRow).also {
            it.label = "Worst frame"
            it.barColor = 0xFFFAB387.toInt()
        }
    }

    override fun onDestroyView() {
        droppedLabel = null; ratioRow = null; worstRow = null
        super.onDestroyView()
    }

    fun update(info: JankInfo) {
        activity?.runOnUiThread {
            droppedLabel?.text = info.droppedFrames.toString()
            ratioRow?.apply {
                fraction = info.jankyFrameRatio
                valueText = "%.1f%%".format(info.jankyFrameRatio * 100)
            }
            worstRow?.apply {
                val cap = 100f
                fraction = (info.worstFrameMs / cap).coerceIn(0f, 1f)
                valueText = "${info.worstFrameMs}ms"
            }
        }
    }
}
