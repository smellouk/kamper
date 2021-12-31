package com.smellouk.kamper.samples

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule

class CpuActivity : AppCompatActivity() {
    private var cpuWorkList: List<CpuWork> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cpu)
        lifecycle.addObserver(Kamper)
        val cpuInfoTxt = findViewById<TextView>(R.id.infoTxt)

        Kamper.apply {
            install(CpuModule)

            addInfoListener<CpuInfo> { cpuInfo ->
                if (cpuInfo == CpuInfo.INVALID) return@addInfoListener

                with(cpuInfo) {
                    cpuInfoTxt.text = "App: ${appRatio.toPercent()}" +
                            "\nTotal: ${totalUseRatio.toPercent()}" +
                            "\nUser: ${userRatio.toPercent()}" +
                            "\nSystem: ${systemRatio.toPercent()}" +
                            "\nIO Wait: ${ioWaitRatio.toPercent()}"
                }
            }
        }

        cpuWorkList = Utils.startHeavyWorkOnBackgroundThread()
    }

    override fun onDestroy() {
        cpuWorkList.forEach { work -> work.cancel(true) }
        super.onDestroy()
    }
}