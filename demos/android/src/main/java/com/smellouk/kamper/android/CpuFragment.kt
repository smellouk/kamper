package com.smellouk.kamper.android

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.android.views.MetricRowView

class CpuFragment : Fragment() {

    private var totalRow: MetricRowView? = null
    private var appRow: MetricRowView? = null
    private var userRow: MetricRowView? = null
    private var systemRow: MetricRowView? = null
    private var ioWaitRow: MetricRowView? = null
    private var loadButton: MaterialButton? = null

    @Volatile private var isLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cpu, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        totalRow = view.findViewById<MetricRowView>(R.id.cpuTotalRow).apply {
            label = "Total"; barColor = 0xFF89B4FA.toInt()
        }
        appRow = view.findViewById<MetricRowView>(R.id.cpuAppRow).apply {
            label = "App"; barColor = 0xFFA6E3A1.toInt()
        }
        userRow = view.findViewById<MetricRowView>(R.id.cpuUserRow).apply {
            label = "User"; barColor = 0xFFF9E2AF.toInt()
        }
        systemRow = view.findViewById<MetricRowView>(R.id.cpuSystemRow).apply {
            label = "System"; barColor = 0xFFFAB387.toInt()
        }
        ioWaitRow = view.findViewById<MetricRowView>(R.id.cpuIoWaitRow).apply {
            label = "IO Wait"; barColor = 0xFFCBA6F7.toInt()
        }
        loadButton = view.findViewById(R.id.cpuLoadButton)
        loadButton?.setOnClickListener { toggleLoad() }
    }

    override fun onDestroyView() {
        isLoading = false
        totalRow = null; appRow = null; userRow = null; systemRow = null; ioWaitRow = null
        loadButton = null
        super.onDestroyView()
    }

    fun update(info: CpuInfo) {
        val act = activity ?: return
        act.runOnUiThread {
            totalRow?.apply { fraction = info.totalUseRatio.toFloat(); valueText = "%.0f%%".format(info.totalUseRatio * 100) }
            appRow?.apply { fraction = info.appRatio.toFloat(); valueText = "%.0f%%".format(info.appRatio * 100) }
            userRow?.apply { fraction = info.userRatio.toFloat(); valueText = "%.0f%%".format(info.userRatio * 100) }
            systemRow?.apply { fraction = info.systemRatio.toFloat(); valueText = "%.0f%%".format(info.systemRatio * 100) }
            ioWaitRow?.apply { fraction = info.ioWaitRatio.toFloat(); valueText = "%.0f%%".format(info.ioWaitRatio * 100) }
        }
    }

    private fun toggleLoad() {
        isLoading = !isLoading
        if (isLoading) {
            Kamper.logEvent("cpu_load_start")
            loadButton?.text = "Stop CPU Load"
            loadButton?.backgroundTintList = ColorStateList.valueOf(0xFFF38BA8.toInt())
            repeat(4) {
                Thread { while (isLoading) { /* spin */ } }.apply { isDaemon = true; start() }
            }
        } else {
            Kamper.logEvent("cpu_load_stop")
            loadButton?.text = "Start CPU Load"
            loadButton?.backgroundTintList = ColorStateList.valueOf(0xFF313244.toInt())
        }
    }
}
