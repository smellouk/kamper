package com.smellouk.kamper.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.smellouk.kamper.Engine
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val HISTORY_SIZE = 60

internal actual class KamperUiRepository(context: Context) {
    private val prefs = (context.applicationContext as Application)
        .getSharedPreferences("kamper_ui_prefs", Context.MODE_PRIVATE)

    private fun loadSettings() = KamperUiSettings(
        showCpu = prefs.getBoolean("show_cpu", true),
        showFps = prefs.getBoolean("show_fps", true),
        showMemory = prefs.getBoolean("show_memory", true),
        showNetwork = prefs.getBoolean("show_network", true)
    )

    private val _settings = MutableStateFlow(loadSettings())
    actual val settings: StateFlow<KamperUiSettings> = _settings.asStateFlow()

    actual fun updateSettings(s: KamperUiSettings) {
        _settings.value = s
        prefs.edit()
            .putBoolean("show_cpu", s.showCpu)
            .putBoolean("show_fps", s.showFps)
            .putBoolean("show_memory", s.showMemory)
            .putBoolean("show_network", s.showNetwork)
            .apply()
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

    private var fpsFrameCount = 0
    private var fpsWindowStartNanos = 0L

    private val fpsCallback: Choreographer.FrameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (fpsWindowStartNanos == 0L) fpsWindowStartNanos = frameTimeNanos
            fpsFrameCount++
            val elapsed = frameTimeNanos - fpsWindowStartNanos
            if (elapsed >= 1_000_000_000L) {
                val fps = (fpsFrameCount * 1_000_000_000L / elapsed).toInt()
                fpsWindowStartNanos = frameTimeNanos
                fpsFrameCount = 0
                fpsHist.push(fps.toFloat())
                _state.update { s ->
                    s.copy(
                        fps = fps,
                        fpsPeak = maxOf(s.fpsPeak, fps),
                        fpsLow = if (s.fpsLow == Int.MAX_VALUE) fps else minOf(s.fpsLow, fps),
                        fpsHistory = fpsHist.toList()
                    )
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        with(engine) {
            install(CpuModule)
            install(MemoryModule(context))
            install(NetworkModule)

            addInfoListener<CpuInfo> { info ->
                if (info == CpuInfo.INVALID) return@addInfoListener
                val v = (info.totalUseRatio * 100).toFloat()
                cpuHist.push(v)
                _state.update { s -> s.copy(cpuPercent = v, cpuHistory = cpuHist.toList()) }
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
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().postFrameCallback(fpsCallback)
        }
    }

    actual fun clear() {
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().removeFrameCallback(fpsCallback)
        }
        engine.clear()
    }
}
