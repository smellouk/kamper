package com.smellouk.kamper.samples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.samples.views.MetricRowView

class GcFragment : Fragment() {

    private var countDeltaLabel: TextView? = null
    private var pauseDeltaRow: MetricRowView? = null
    private var totalCountRow: MetricRowView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gc, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        countDeltaLabel = view.findViewById(R.id.gcCountDeltaLabel)
        pauseDeltaRow = view.findViewById<MetricRowView>(R.id.gcPauseDeltaRow).also {
            it.label = "GC pause"
            it.barColor = 0xFF94E2D5.toInt()
        }
        totalCountRow = view.findViewById<MetricRowView>(R.id.gcTotalCountRow).also {
            it.label = "Total GCs"
            it.barColor = 0xFF94E2D5.toInt()
        }
    }

    override fun onDestroyView() {
        countDeltaLabel = null; pauseDeltaRow = null; totalCountRow = null
        super.onDestroyView()
    }

    fun update(info: GcInfo) {
        activity?.runOnUiThread {
            countDeltaLabel?.text = info.gcCountDelta.toString()
            pauseDeltaRow?.apply {
                val cap = 100f
                fraction = (info.gcPauseMsDelta / cap).coerceIn(0f, 1f)
                valueText = "${info.gcPauseMsDelta}ms"
            }
            totalCountRow?.apply {
                val cap = 1000f
                fraction = (info.gcCount / cap).coerceIn(0f, 1f)
                valueText = info.gcCount.toString()
            }
        }
    }
}
