package com.smellouk.konitor.compose

import com.smellouk.konitor.Konitor
import com.smellouk.konitor.api.UserEventInfo
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.gc.GcModule
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.GpuModule
import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.JankModule
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual val appTitle: String = "K|Desktop|Compose"

actual fun KonitorState.initialize(scope: CoroutineScope) {
    Konitor.install(CpuModule)
    Konitor.install(GpuModule)
    Konitor.install(FpsModule)
    Konitor.install(MemoryModule())
    Konitor.install(NetworkModule)
    Konitor.install(IssuesModule(anr = AnrConfig()) {
        crash { chainToPreviousHandler = false }
    })
    Konitor.install(JankModule)
    Konitor.install(GcModule)
    Konitor.install(ThermalModule)

    Konitor.addInfoListener<CpuInfo>     { info -> if (info != CpuInfo.INVALID) scope.launch { cpuInfo = info } }
    Konitor.addInfoListener<GpuInfo>     { info -> if (info != GpuInfo.INVALID) scope.launch { gpuInfo = info } }
    Konitor.addInfoListener<FpsInfo>     { info -> if (info != FpsInfo.INVALID) scope.launch { fpsInfo = info } }
    Konitor.addInfoListener<MemoryInfo>  { info -> if (info != MemoryInfo.INVALID) scope.launch { memoryInfo = info } }
    Konitor.addInfoListener<NetworkInfo> { info -> if (info != NetworkInfo.INVALID) scope.launch { networkInfo = info } }
    Konitor.addInfoListener<IssueInfo>   { info -> scope.launch { addIssue(info.issue) } }
    Konitor.addInfoListener<JankInfo>    { info -> if (info != JankInfo.INVALID) scope.launch { jankInfo = info } }
    Konitor.addInfoListener<GcInfo>      { info -> if (info != GcInfo.INVALID) scope.launch { gcInfo = info } }
    Konitor.addInfoListener<ThermalInfo>   { info -> if (info != ThermalInfo.INVALID) scope.launch { thermalInfo = info } }
    Konitor.addInfoListener<UserEventInfo> { info -> if (info != UserEventInfo.INVALID) scope.launch { addUserEvent(info) } }
}

actual fun startKonitor() = Konitor.start()
actual fun stopKonitor() = Konitor.stop()
actual fun disposeKonitor() {
    Konitor.stop()
    Konitor.clear()
}

actual fun platformSupportsAppTraffic(): Boolean = false

actual fun currentTimeMs(): Long = System.currentTimeMillis()
