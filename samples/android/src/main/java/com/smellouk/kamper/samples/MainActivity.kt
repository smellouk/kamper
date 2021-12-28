package com.smellouk.kamper.samples

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.api.DEFAULT
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule

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

            install(
                FpsModule {
                    isEnabled = true
                    logger = Logger.DEFAULT
                }
            )
            // Quick start default(isEnabled=true, logger=Logger.EMPTY)
            // install(FpsModule)

            install(
                MemoryModule(applicationContext) {
                    isEnabled = true
                    intervalInMs = 1000
                    logger = Logger.DEFAULT
                }
            )

            install(
                NetworkModule {
                    isEnabled = true
                    intervalInMs = 1000
                    logger = Logger.DEFAULT
                }
            )

            addInfoListener<CpuInfo> { cpuInfo ->
                if (cpuInfo != CpuInfo.INVALID) {
                    Log.i("Samples", cpuInfo.toString())
                } else {
                    Log.i("Samples", "CPU info is invalid")
                }
            }

            addInfoListener<FpsInfo> { fpsInfo ->
                if (fpsInfo != FpsInfo.INVALID) {
                    Log.i("Samples", fpsInfo.toString())
                } else {
                    Log.i("Samples", "FPS info is invalid")
                }
            }

            addInfoListener<MemoryInfo> { memoryInfo ->
                if (memoryInfo != MemoryInfo.INVALID) {
                    Log.i("Samples", memoryInfo.toString())
                } else {
                    Log.i("Samples", "Memory info is invalid")
                }
            }

            addInfoListener<NetworkInfo> { memoryInfo ->
                if (memoryInfo != NetworkInfo.INVALID) {
                    Log.i("Samples", memoryInfo.toString())
                } else {
                    Log.i("Samples", "Network info is invalid")
                }
            }
        }
    }
}