package com.smellouk.konitor.android

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule

class CpuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cpu)
        lifecycle.addObserver(Konitor)
        val cpuInfoTxt = findViewById<TextView>(R.id.infoTxt)

        Konitor.apply {
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

        Utils.startHeavyWorkOnBackgroundThread()
    }
}