package com.smellouk.kamper.samples

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.api.DEFAULT
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycle.addObserver(Kamper)

        Kamper.setup {
            logger = Logger.DEFAULT
        }.apply {
            install(
                CpuModule {
                    isEnabled = true
                    intervalInMs = 1000
                    logger = Logger.DEFAULT
                }
            )
            // Quick start default(isEnabled=true, intervalInMs=1000, logger=Logger.EMPTY)
            // install(CpuModule)

            addInfoListener<CpuInfo> { cpuInfo ->
                if (cpuInfo != CpuInfo.INVALID) {
                    Log.i("Samples", cpuInfo.toString())
                } else {
                    Log.i("Samples", "CPU info is invalid")
                }
            }
        }
    }
}