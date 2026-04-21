package com.smellouk.kamper.samples.compose

import android.content.Context
import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.gc.GcModule
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

internal var appContext: Context? = null

actual fun KamperState.initialize(scope: CoroutineScope) {
    val ctx = checkNotNull(appContext) { "appContext must be set before initializing Kamper" }
    val app = ctx.applicationContext as android.app.Application
    Kamper.install(CpuModule)
    Kamper.install(FpsModule)
    Kamper.install(MemoryModule(ctx))
    Kamper.install(NetworkModule)
    Kamper.install(IssuesModule(ctx, anr = AnrConfig(thresholdMs = 5_000L)) {
        crash { chainToPreviousHandler = false }
    })
    Kamper.install(JankModule(app))
    Kamper.install(GcModule)
    Kamper.install(ThermalModule(ctx))

    Kamper.addInfoListener<CpuInfo>     { info -> scope.launch { cpuInfo = info } }
    Kamper.addInfoListener<FpsInfo>     { info -> scope.launch { fpsInfo = info } }
    Kamper.addInfoListener<MemoryInfo>  { info -> scope.launch { memoryInfo = info } }
    Kamper.addInfoListener<NetworkInfo> { info -> scope.launch { networkInfo = info } }
    Kamper.addInfoListener<IssueInfo>   { info -> scope.launch { addIssue(info.issue) } }
    Kamper.addInfoListener<JankInfo>    { info -> scope.launch { jankInfo = info } }
    Kamper.addInfoListener<GcInfo>      { info -> scope.launch { gcInfo = info } }
    Kamper.addInfoListener<ThermalInfo> { info -> scope.launch { thermalInfo = info } }
}

actual fun startKamper() = Kamper.start()
actual fun stopKamper() = Kamper.stop()
actual fun disposeKamper() {
    Kamper.stop()
    Kamper.clear()
}
