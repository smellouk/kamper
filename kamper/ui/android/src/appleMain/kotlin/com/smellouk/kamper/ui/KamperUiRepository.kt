package com.smellouk.kamper.ui

import com.smellouk.kamper.Engine
import platform.Foundation.NSUserDefaults
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val HISTORY_SIZE = 60

internal actual class KamperUiRepository {
    private fun loadSettings(): KamperUiSettings {
        val d = NSUserDefaults.standardUserDefaults
        fun bool(key: String) = if (d.objectForKey(key) != null) d.boolForKey(key) else true
        return KamperUiSettings(
            showCpu = bool("show_cpu"),
            showFps = bool("show_fps"),
            showMemory = bool("show_memory"),
            showNetwork = bool("show_network")
        )
    }

    private val _settings = MutableStateFlow(loadSettings())
    actual val settings: StateFlow<KamperUiSettings> = _settings.asStateFlow()

    actual fun updateSettings(s: KamperUiSettings) {
        _settings.value = s
        NSUserDefaults.standardUserDefaults.apply {
            setBool(s.showCpu, "show_cpu")
            setBool(s.showFps, "show_fps")
            setBool(s.showMemory, "show_memory")
            setBool(s.showNetwork, "show_network")
        }
    }

    private val engine = Engine()
    private val _state = MutableStateFlow(KamperUiState.EMPTY)
    actual val state: StateFlow<KamperUiState> = _state.asStateFlow()

    private val cpuHist = ArrayDeque<Float>()
    private val fpsHist = ArrayDeque<Float>()
    private val memHist = ArrayDeque<Float>()
    private val netHist = ArrayDeque<Float>()

    private fun ArrayDeque<Float>.push(v: Float) {
        if (size >= HISTORY_SIZE) removeFirst()
        addLast(v)
    }

    init {
        with(engine) {
            install(CpuModule)
            install(FpsModule)
            install(MemoryModule())
            install(NetworkModule)

            addInfoListener<CpuInfo> { info ->
                if (info == CpuInfo.INVALID) return@addInfoListener
                val v = (info.totalUseRatio * 100).toFloat()
                cpuHist.push(v)
                _state.update { s -> s.copy(cpuPercent = v, cpuHistory = cpuHist.toList()) }
            }

            addInfoListener<FpsInfo> { info ->
                if (info == FpsInfo.INVALID) return@addInfoListener
                val v = info.fps
                fpsHist.push(v.toFloat())
                _state.update { s ->
                    s.copy(
                        fps = v,
                        fpsPeak = maxOf(s.fpsPeak, v),
                        fpsLow = if (s.fpsLow == Int.MAX_VALUE) v else minOf(s.fpsLow, v),
                        fpsHistory = fpsHist.toList()
                    )
                }
            }

            addInfoListener<MemoryInfo> { info ->
                if (info == MemoryInfo.INVALID) return@addInfoListener
                val v = info.heapMemoryInfo.allocatedInMb
                memHist.push(v)
                _state.update { s -> s.copy(memoryUsedMb = v, memoryHistory = memHist.toList()) }
            }

            addInfoListener<NetworkInfo> { info ->
                if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@addInfoListener
                val v = info.rxSystemTotalInMb
                netHist.push(v)
                _state.update { s -> s.copy(downloadMbps = v, downloadHistory = netHist.toList()) }
            }

            start()
        }
    }

    actual fun clear() = engine.clear()
}
