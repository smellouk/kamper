package com.smellouk.konitor.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.android.views.MetricRowView

class MemoryFragment : Fragment() {

    private var heapRow: MetricRowView? = null
    private var heapDetail: TextView? = null
    private var ramRow: MetricRowView? = null
    private var ramDetail: TextView? = null
    private var pssDetail: TextView? = null
    private var lowMemBadge: TextView? = null

    private val allocations = mutableListOf<ByteArray>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_memory, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        heapRow = view.findViewById<MetricRowView>(R.id.heapRow).apply {
            label = "Heap"; barColor = 0xFFA6E3A1.toInt()
        }
        ramRow = view.findViewById<MetricRowView>(R.id.ramRow).apply {
            label = "RAM"; barColor = 0xFF89B4FA.toInt()
        }
        heapDetail = view.findViewById(R.id.heapDetail)
        ramDetail = view.findViewById(R.id.ramDetail)
        pssDetail = view.findViewById(R.id.pssDetail)
        lowMemBadge = view.findViewById(R.id.lowMemBadge)

        view.findViewById<View>(R.id.allocButton).setOnClickListener {
            Konitor.logEvent("memory_alloc_32mb")
            allocations.add(ByteArray(32 * 1024 * 1024))
        }
        view.findViewById<View>(R.id.gcButton).setOnClickListener {
            Konitor.logEvent("memory_gc")
            allocations.clear(); System.gc()
        }
    }

    override fun onDestroyView() {
        heapRow = null; heapDetail = null; ramRow = null; ramDetail = null
        pssDetail = null; lowMemBadge = null
        super.onDestroyView()
    }

    fun update(info: MemoryInfo) {
        val act = activity ?: return
        act.runOnUiThread {
            with(info.heapMemoryInfo) {
                val frac = if (maxMemoryInMb > 0) allocatedInMb / maxMemoryInMb else 0f
                heapRow?.fraction = frac
                heapRow?.valueText = "%.0f%%".format(frac * 100)
                heapDetail?.text = "%.1f MB  /  %.1f MB max".format(allocatedInMb, maxMemoryInMb)
            }
            with(info.ramInfo) {
                val used = totalRamInMb - availableRamInMb
                val frac = if (totalRamInMb > 0) used / totalRamInMb else 0f
                ramRow?.fraction = frac
                ramRow?.valueText = "%.0f%%".format(frac * 100)
                ramDetail?.text = "%.0f MB  /  %.0f MB total".format(used, totalRamInMb)
                lowMemBadge?.text = if (isLowMemory) "⚠ Low Memory" else ""
            }
            with(info.pssInfo) {
                if (totalPssInMb >= 0f) {
                    pssDetail?.text = "Total: %.1f MB   Dalvik: %.1f   Native: %.1f   Other: %.1f".format(
                        totalPssInMb, dalvikPssInMb, nativePssInMb, otherPssInMb
                    )
                }
            }
        }
    }
}
