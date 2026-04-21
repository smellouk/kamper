package com.smellouk.kamper

import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule

class KamperBridge {
    private val engine = Engine()

    fun setup(
        onCpu: (CpuInfo) -> Unit,
        onFps: (FpsInfo) -> Unit,
        onMemory: (MemoryInfo) -> Unit,
        onNetwork: (NetworkInfo) -> Unit
    ) {
        with(engine) {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)
            addInfoListener<CpuInfo>(onCpu)
            addInfoListener<FpsInfo>(onFps)
            addInfoListener<MemoryInfo>(onMemory)
            addInfoListener<NetworkInfo>(onNetwork)
        }
    }

    fun start() = engine.start()
    fun stop() = engine.stop()
    fun clear() = engine.clear()
}
