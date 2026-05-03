package com.smellouk.kamper.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.gpu.GpuInfo

class GpuFragment : Fragment() {

    private var utilizationLabel: TextView? = null
    private var memoryValue: TextView? = null
    private var curFreqValue: TextView? = null
    private var maxFreqValue: TextView? = null
    private var appUtilValue: TextView? = null
    private var rendererUtilValue: TextView? = null
    private var tilerUtilValue: TextView? = null
    private var computeUtilValue: TextView? = null
    private var stressButton: MaterialButton? = null
    private var stressView: GpuStressView? = null
    private var isStressing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gpu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        utilizationLabel  = view.findViewById(R.id.gpuUtilizationLabel)
        memoryValue       = view.findViewById(R.id.gpuMemoryValue)
        curFreqValue      = view.findViewById(R.id.gpuCurFreqValue)
        maxFreqValue      = view.findViewById(R.id.gpuMaxFreqValue)
        appUtilValue      = view.findViewById(R.id.gpuAppUtilValue)
        rendererUtilValue = view.findViewById(R.id.gpuRendererUtilValue)
        tilerUtilValue    = view.findViewById(R.id.gpuTilerUtilValue)
        computeUtilValue  = view.findViewById(R.id.gpuComputeUtilValue)
        stressView        = view.findViewById(R.id.gpuStressView)
        stressButton      = view.findViewById(R.id.gpuStressButton)
        stressButton?.setOnClickListener { toggleStress() }
    }

    override fun onResume() {
        super.onResume()
        if (isStressing) stressView?.resumeRenderer()
    }

    override fun onPause() {
        if (isStressing) stressView?.pauseRenderer()
        super.onPause()
    }

    override fun onDestroyView() {
        stopStress()
        utilizationLabel = null; memoryValue = null
        curFreqValue = null; maxFreqValue = null
        appUtilValue = null; rendererUtilValue = null
        tilerUtilValue = null; computeUtilValue = null
        stressButton = null; stressView = null
        super.onDestroyView()
    }

    private fun toggleStress() {
        if (isStressing) stopStress() else startStress()
    }

    private fun startStress() {
        Kamper.logEvent("gpu_stress_start")
        isStressing = true
        stressView?.start()
        stressButton?.text = "STOP STRESS"
    }

    private fun stopStress() {
        Kamper.logEvent("gpu_stress_stop")
        isStressing = false
        stressView?.stop()
        stressButton?.text = "STRESS GPU"
    }

    fun update(info: GpuInfo) {
        activity?.runOnUiThread {
            if (info == GpuInfo.UNSUPPORTED) {
                utilizationLabel?.text = "UNSUPPORTED"
                utilizationLabel?.setTextColor(COLOR_UNSUPPORTED)
                memoryValue?.text = "N/A"
                curFreqValue?.text = "N/A"
                maxFreqValue?.text = "N/A"
                appUtilValue?.text = "N/A"
                rendererUtilValue?.text = "N/A"
                tilerUtilValue?.text = "N/A"
                computeUtilValue?.text = "N/A"
            } else {
                utilizationLabel?.text = if (info.utilization >= 0.0) {
                    "%.1f%%".format(info.utilization)
                } else {
                    "—%"
                }
                utilizationLabel?.setTextColor(COLOR_MAUVE)
                memoryValue?.text = formatMemory(info.usedMemoryMb, info.totalMemoryMb)
                curFreqValue?.text = formatFreq(info.curFreqKhz)
                maxFreqValue?.text = formatFreq(info.maxFreqKhz)
                appUtilValue?.text = formatUtil(info.appUtilization)
                rendererUtilValue?.text = formatUtil(info.rendererUtilization)
                tilerUtilValue?.text = formatUtil(info.tilerUtilization)
                computeUtilValue?.text = formatUtil(info.computeUtilization)
            }
        }
    }

    private fun formatMemory(usedMb: Double, totalMb: Double): String = when {
        usedMb >= 0.0 && totalMb >= 0.0 -> "%.0f MB / %.0f MB".format(usedMb, totalMb)
        totalMb >= 0.0                   -> "— / %.0f MB".format(totalMb)
        else                             -> "N/A"
    }

    private fun formatFreq(khz: Long): String = when {
        khz >= 0L -> "%.0f MHz".format(khz / KHZ_PER_MHZ)
        else      -> "—"
    }

    private fun formatUtil(value: Double): String = when {
        value >= 0.0 -> "%.1f%%".format(value)
        else         -> "N/A"
    }

    private companion object {
        val COLOR_UNSUPPORTED: Int = 0xFFA6ADC8.toInt()
        val COLOR_MAUVE: Int = 0xFFCBA6F7.toInt()
        const val KHZ_PER_MHZ = 1000.0
    }
}
