package com.smellouk.kamper.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Trace
import android.view.Choreographer
import com.smellouk.kamper.Engine
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HISTORY_SIZE = 60
private const val MAX_ISSUES = 100
private const val PREF_ISSUES = "issues_list"

internal actual class KamperUiRepository(context: Context) {
    private val prefs = (context.applicationContext as Application)
        .getSharedPreferences("kamper_ui_prefs", Context.MODE_PRIVATE)

    private fun loadSettings() = KamperUiSettings(
        showCpu     = prefs.getBoolean("show_cpu", true),
        showFps     = prefs.getBoolean("show_fps", true),
        showMemory  = prefs.getBoolean("show_memory", true),
        showNetwork = prefs.getBoolean("show_network", true),
        showIssues  = prefs.getBoolean("show_issues", true)
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
            .putBoolean("show_issues", s.showIssues)
            .apply()
    }

    private val engine = Engine()
    private val _state = MutableStateFlow(KamperUiState.EMPTY)
    actual val state: StateFlow<KamperUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val perfettoCapture = PerfettoCapture(context)

    private val cpuHist = ArrayDeque<Float>()
    private val fpsHist = ArrayDeque<Float>()
    private val memHist = ArrayDeque<Float>()
    private val netHist = ArrayDeque<Float>()

    private fun ArrayDeque<Float>.push(v: Float) {
        if (size >= HISTORY_SIZE) removeFirst()
        addLast(v)
    }

    private val issuesList = loadPersistedIssues()

    private fun loadPersistedIssues(): MutableList<com.smellouk.kamper.issues.Issue> {
        val raw = prefs.getString(PREF_ISSUES, "") ?: return mutableListOf()
        return raw.lines().mapNotNull { it.deserializeIssue() }.toMutableList()
    }

    private fun saveIssues() {
        prefs.edit().putString(PREF_ISSUES, issuesList.joinToString("\n") { it.serialize() }).apply()
    }

    actual fun clearIssues() {
        issuesList.clear()
        saveIssues()
        _state.update { it.copy(issues = emptyList(), unreadIssueCount = 0) }
    }

    actual fun startCapture() {
        _state.update { it.copy(isRecordingTrace = true, traceSpans = emptyList(), traceFilePath = null, traceStatus = null) }
        scope.launch {
            val err = perfettoCapture.start()
            if (err != null) {
                _state.update { it.copy(isRecordingTrace = false, traceStatus = err) }
            }
        }
    }

    actual fun stopCapture() {
        _state.update { it.copy(isRecordingTrace = false, isProcessingTrace = true) }
        scope.launch {
            perfettoCapture.stop()
            val file = perfettoCapture.traceFile()
            val spans = if (file != null) PerfettoParser.parse(file) else emptyList()
            val perfettoErr = perfettoCapture.lastError()
            val status = when {
                file == null && perfettoErr.isNotEmpty() -> perfettoErr
                file == null -> "No trace file — perfetto may have failed to start"
                file.length() == 0L -> "Trace file empty — try recording for a few seconds"
                spans.isEmpty() -> "Captured ${file.length() / 1024}KB — no ATrace spans found"
                else -> null
            }
            _state.update { it.copy(
                isProcessingTrace = false,
                traceSpans = spans,
                traceFilePath = file?.absolutePath,
                traceStatus = status
            )}
        }
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
                        fps     = fps,
                        fpsPeak = maxOf(s.fpsPeak, fps),
                        fpsLow  = if (s.fpsLow == Int.MAX_VALUE) fps else minOf(s.fpsLow, fps),
                        fpsHistory = fpsHist.toList()
                    )
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        if (issuesList.isNotEmpty()) {
            _state.update { it.copy(issues = issuesList.toList()) }
        }

        with(engine) {
            install(CpuModule)
            install(MemoryModule(context))
            install(NetworkModule)
            install(IssuesModule(context, anr = AnrConfig()) { crash { chainToPreviousHandler = false } })

            addInfoListener<CpuInfo> { info ->
                if (info == CpuInfo.INVALID) return@addInfoListener
                Trace.beginSection("kamper.cpu")
                val v = (info.totalUseRatio * 100).toFloat()
                cpuHist.push(v)
                _state.update { s -> s.copy(cpuPercent = v, cpuHistory = cpuHist.toList()) }
                Trace.endSection()
            }

            addInfoListener<MemoryInfo> { info ->
                if (info == MemoryInfo.INVALID) return@addInfoListener
                Trace.beginSection("kamper.memory")
                val v = info.heapMemoryInfo.allocatedInMb
                memHist.push(v)
                _state.update { s -> s.copy(memoryUsedMb = v, memoryHistory = memHist.toList()) }
                Trace.endSection()
            }

            addInfoListener<NetworkInfo> { info ->
                if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@addInfoListener
                Trace.beginSection("kamper.network")
                val v = info.rxSystemTotalInMb
                netHist.push(v)
                _state.update { s -> s.copy(downloadMbps = v, downloadHistory = netHist.toList()) }
                Trace.endSection()
            }

            addInfoListener<IssueInfo> { info ->
                if (info == IssueInfo.INVALID) return@addInfoListener
                Trace.beginSection("Kamper:${info.issue.type.name}")
                Trace.endSection()
                issuesList.add(0, info.issue)
                if (issuesList.size > MAX_ISSUES) issuesList.removeAt(issuesList.size - 1)
                saveIssues()
                _state.update { s ->
                    s.copy(
                        issues           = issuesList.toList(),
                        unreadIssueCount = s.unreadIssueCount + 1
                    )
                }
            }

            start()
        }
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().postFrameCallback(fpsCallback)
        }
    }

    actual fun clear() {
        scope.cancel()
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().removeFrameCallback(fpsCallback)
        }
        engine.clear()
    }
}
