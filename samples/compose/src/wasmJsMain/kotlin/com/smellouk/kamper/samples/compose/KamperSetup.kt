package com.smellouk.kamper.samples.compose

import com.smellouk.kamper.Kamper
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual fun KamperState.initialize(scope: CoroutineScope) {
    Kamper.install(CpuModule)
    Kamper.install(FpsModule)
    Kamper.install(MemoryModule())
    Kamper.install(NetworkModule)
    Kamper.install(IssuesModule())

    Kamper.addInfoListener<CpuInfo>    { info -> scope.launch { cpuInfo = info } }
    Kamper.addInfoListener<FpsInfo>    { info -> scope.launch { fpsInfo = info } }
    Kamper.addInfoListener<MemoryInfo> { info -> scope.launch { memoryInfo = info } }
    Kamper.addInfoListener<NetworkInfo>{ info -> scope.launch { networkInfo = info } }
    Kamper.addInfoListener<IssueInfo>  { info -> scope.launch { addIssue(info.issue) } }
}

actual fun startKamper() = Kamper.start()
actual fun stopKamper() = Kamper.stop()
actual fun disposeKamper() {
    Kamper.stop()
    Kamper.clear()
}
