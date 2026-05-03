package com.smellouk.konitor

import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.FpsModule
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssueSpans
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class KonitorBridge {
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

    fun setupIssues(onIssue: (IssueInfo) -> Unit) = installAndListenIssues(onIssue)

    fun simulateSlowSpan() {
        val span = IssueSpans.begin("demo_slow_span")
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            delay(1_100L)
            span.end()
        }
    }

    fun start() = engine.start()
    fun stop() = engine.stop()
    fun clear() = engine.clear()
}

internal expect fun installAndListenIssues(onIssue: (IssueInfo) -> Unit)
