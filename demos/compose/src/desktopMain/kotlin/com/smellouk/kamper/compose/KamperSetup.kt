package com.smellouk.kamper.compose

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.api.UserEventInfo
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.gc.GcModule
import com.smellouk.kamper.gpu.GpuInfo
import com.smellouk.kamper.gpu.GpuModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.jank.JankModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual val appTitle: String = "K|Desktop|Compose"

actual fun KamperState.initialize(scope: CoroutineScope) {
    Kamper.install(CpuModule)
    Kamper.install(GpuModule)
    Kamper.install(FpsModule)
    Kamper.install(MemoryModule())
    Kamper.install(NetworkModule)
    Kamper.install(IssuesModule(anr = AnrConfig()) {
        crash { chainToPreviousHandler = false }
    })
    Kamper.install(JankModule)
    Kamper.install(GcModule)
    Kamper.install(ThermalModule)

    Kamper.addInfoListener<CpuInfo>     { info -> if (info != CpuInfo.INVALID) scope.launch { cpuInfo = info } }
    Kamper.addInfoListener<GpuInfo>     { info -> if (info != GpuInfo.INVALID) scope.launch { gpuInfo = info } }
    Kamper.addInfoListener<FpsInfo>     { info -> if (info != FpsInfo.INVALID) scope.launch { fpsInfo = info } }
    Kamper.addInfoListener<MemoryInfo>  { info -> if (info != MemoryInfo.INVALID) scope.launch { memoryInfo = info } }
    Kamper.addInfoListener<NetworkInfo> { info -> if (info != NetworkInfo.INVALID) scope.launch { networkInfo = info } }
    Kamper.addInfoListener<IssueInfo>   { info -> scope.launch { addIssue(info.issue) } }
    Kamper.addInfoListener<JankInfo>    { info -> if (info != JankInfo.INVALID) scope.launch { jankInfo = info } }
    Kamper.addInfoListener<GcInfo>      { info -> if (info != GcInfo.INVALID) scope.launch { gcInfo = info } }
    Kamper.addInfoListener<ThermalInfo>   { info -> if (info != ThermalInfo.INVALID) scope.launch { thermalInfo = info } }
    Kamper.addInfoListener<UserEventInfo> { info -> if (info != UserEventInfo.INVALID) scope.launch { addUserEvent(info) } }
}

actual fun startKamper() = Kamper.start()
actual fun stopKamper() = Kamper.stop()
actual fun disposeKamper() {
    Kamper.stop()
    Kamper.clear()
}

actual fun platformSupportsAppTraffic(): Boolean = false

actual fun currentTimeMs(): Long = System.currentTimeMillis()
