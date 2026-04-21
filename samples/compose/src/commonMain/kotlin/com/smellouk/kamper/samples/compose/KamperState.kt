package com.smellouk.kamper.samples.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.network.NetworkInfo
import kotlinx.coroutines.CoroutineScope

class KamperState {
    var cpuInfo by mutableStateOf(CpuInfo.INVALID)
    var fpsInfo by mutableStateOf(FpsInfo.INVALID)
    var memoryInfo by mutableStateOf(MemoryInfo.INVALID)
    var networkInfo by mutableStateOf(NetworkInfo.INVALID)
    var isRunning by mutableStateOf(false)
}

expect fun KamperState.initialize(scope: CoroutineScope)
expect fun startKamper()
expect fun stopKamper()
expect fun disposeKamper()
