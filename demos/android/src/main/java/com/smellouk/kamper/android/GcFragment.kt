package com.smellouk.kamper.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.gc.GcInfo

class GcFragment : Fragment() {

    private var countDeltaLabel: TextView? = null
    private var pauseValue: TextView? = null
    private var totalValue: TextView? = null
    private var simulateButton: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gc, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        countDeltaLabel = view.findViewById(R.id.gcCountDeltaLabel)
        pauseValue      = view.findViewById(R.id.gcPauseValue)
        totalValue      = view.findViewById(R.id.gcTotalValue)
        simulateButton  = view.findViewById(R.id.gcSimulateButton)
        simulateButton?.setOnClickListener { simulateGc() }
    }

    override fun onDestroyView() {
        countDeltaLabel = null; pauseValue = null; totalValue = null; simulateButton = null
        super.onDestroyView()
    }

    fun update(info: GcInfo) {
        activity?.runOnUiThread {
            countDeltaLabel?.text = info.gcCountDelta.toString()
            pauseValue?.text      = "${info.gcPauseMsDelta} ms"
            totalValue?.text      = info.gcCount.toString()
        }
    }

    // Allocate and discard large arrays to pressure the GC
    private fun simulateGc() {
        Kamper.logEvent("gc_simulate")
        Thread {
            repeat(200_000) { ByteArray(1024) }
        }.apply { isDaemon = true; start() }
    }
}
