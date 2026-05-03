package com.smellouk.konitor.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.jank.JankInfo

class JankFragment : Fragment() {

    private var droppedLabel: TextView? = null
    private var ratioValue: TextView? = null
    private var worstValue: TextView? = null
    private var simulateButton: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_jank, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        droppedLabel   = view.findViewById(R.id.jankDroppedLabel)
        ratioValue     = view.findViewById(R.id.jankRatioValue)
        worstValue     = view.findViewById(R.id.jankWorstValue)
        simulateButton = view.findViewById(R.id.jankSimulateButton)
        simulateButton?.setOnClickListener { simulateJank() }
    }

    override fun onDestroyView() {
        droppedLabel = null; ratioValue = null; worstValue = null; simulateButton = null
        super.onDestroyView()
    }

    fun update(info: JankInfo) {
        activity?.runOnUiThread {
            droppedLabel?.text = info.droppedFrames.toString()
            ratioValue?.text   = "%.1f%%".format(info.jankyFrameRatio * 100)
            worstValue?.text   = "${info.worstFrameMs} ms"
        }
    }

    // Block the main thread briefly to force dropped frames
    private fun simulateJank() {
        Konitor.logEvent("jank_simulate")
        val end = System.nanoTime() + 200_000_000L
        @Suppress("ControlFlowWithEmptyBody")
        while (System.nanoTime() < end) {}
    }
}
