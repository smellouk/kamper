package com.smellouk.kamper.ui

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.smellouk.kamper.Engine
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.cpu.CpuConfig
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.CrashConfig
import com.smellouk.kamper.issues.DroppedFramesConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesConfig
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.issues.MemoryPressureConfig
import com.smellouk.kamper.issues.SlowSpanConfig
import com.smellouk.kamper.issues.SlowStartConfig
import com.smellouk.kamper.gc.GcConfig
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.gc.GcModule
import com.smellouk.kamper.jank.JankConfig
import com.smellouk.kamper.jank.JankInfo
import com.smellouk.kamper.jank.JankModule
import com.smellouk.kamper.memory.MemoryConfig
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryModule
import com.smellouk.kamper.network.NetworkConfig
import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.NetworkModule
import com.smellouk.kamper.thermal.ThermalConfig
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val HISTORY_SIZE = 60
private const val MAX_ISSUES = 100
private const val PREF_ISSUES = "issues_list"
private const val MAX_RECORDING_SAMPLES = 4_200 // ~10 min at 7 metrics/s

internal actual class KamperUiRepository(context: Context) {
    private val appContext = context.applicationContext as Application
    private val prefs = appContext.getSharedPreferences("kamper_ui_prefs", Context.MODE_PRIVATE)

    // ── Settings persistence ──────────────────────────────────────────────────

    private fun loadSettings() = KamperUiSettings(
        showCpu                        = prefs.getBoolean("show_cpu", true),
        showFps                        = prefs.getBoolean("show_fps", true),
        showMemory                     = prefs.getBoolean("show_memory", true),
        showNetwork                    = prefs.getBoolean("show_network", true),
        showIssues                     = prefs.getBoolean("show_issues", true),
        cpuEnabled                     = prefs.getBoolean("cpu_enabled", true),
        fpsEnabled                     = prefs.getBoolean("fps_enabled", true),
        memoryEnabled                  = prefs.getBoolean("memory_enabled", true),
        networkEnabled                 = prefs.getBoolean("network_enabled", true),
        issuesEnabled                  = prefs.getBoolean("issues_enabled", true),
        cpuIntervalMs                  = prefs.getLong("cpu_interval_ms", 1_000L),
        memoryIntervalMs               = prefs.getLong("memory_interval_ms", 1_000L),
        networkIntervalMs              = prefs.getLong("network_interval_ms", 1_000L),
        issuesIntervalMs               = prefs.getLong("issues_interval_ms", 1_000L),
        slowSpanEnabled                = prefs.getBoolean("slow_span_enabled", true),
        slowSpanThresholdMs            = prefs.getLong("slow_span_threshold_ms", 1_000L),
        droppedFramesEnabled           = prefs.getBoolean("dropped_frames_enabled", true),
        droppedFrameThresholdMs        = prefs.getLong("dropped_frame_threshold_ms", 32L),
        droppedFrameConsecutiveThreshold = prefs.getInt("dropped_frame_consecutive", 3),
        crashEnabled                   = prefs.getBoolean("crash_enabled", true),
        memoryPressureEnabled          = prefs.getBoolean("mem_pressure_enabled", true),
        memPressureWarningPct          = prefs.getFloat("mem_pressure_warning_pct", 0.80f),
        memPressureCriticalPct         = prefs.getFloat("mem_pressure_critical_pct", 0.95f),
        anrEnabled                     = prefs.getBoolean("anr_enabled", true),
        anrThresholdMs                 = prefs.getLong("anr_threshold_ms", 5_000L),
        slowStartEnabled               = prefs.getBoolean("slow_start_enabled", true),
        slowStartColdThresholdMs       = prefs.getLong("slow_start_cold_ms", 2_000L),
        slowStartWarmThresholdMs       = prefs.getLong("slow_start_warm_ms", 800L),
        showJank                       = prefs.getBoolean("show_jank", true),
        showGc                         = prefs.getBoolean("show_gc", true),
        showThermal                    = prefs.getBoolean("show_thermal", true),
        jankEnabled                    = prefs.getBoolean("jank_enabled", true),
        gcEnabled                      = prefs.getBoolean("gc_enabled", true),
        thermalEnabled                 = prefs.getBoolean("thermal_enabled", true)
    )

    private fun saveSettings(s: KamperUiSettings) {
        prefs.edit()
            .putBoolean("show_cpu", s.showCpu)
            .putBoolean("show_fps", s.showFps)
            .putBoolean("show_memory", s.showMemory)
            .putBoolean("show_network", s.showNetwork)
            .putBoolean("show_issues", s.showIssues)
            .putBoolean("cpu_enabled", s.cpuEnabled)
            .putBoolean("fps_enabled", s.fpsEnabled)
            .putBoolean("memory_enabled", s.memoryEnabled)
            .putBoolean("network_enabled", s.networkEnabled)
            .putBoolean("issues_enabled", s.issuesEnabled)
            .putLong("cpu_interval_ms", s.cpuIntervalMs)
            .putLong("memory_interval_ms", s.memoryIntervalMs)
            .putLong("network_interval_ms", s.networkIntervalMs)
            .putLong("issues_interval_ms", s.issuesIntervalMs)
            .putBoolean("slow_span_enabled", s.slowSpanEnabled)
            .putLong("slow_span_threshold_ms", s.slowSpanThresholdMs)
            .putBoolean("dropped_frames_enabled", s.droppedFramesEnabled)
            .putLong("dropped_frame_threshold_ms", s.droppedFrameThresholdMs)
            .putInt("dropped_frame_consecutive", s.droppedFrameConsecutiveThreshold)
            .putBoolean("crash_enabled", s.crashEnabled)
            .putBoolean("mem_pressure_enabled", s.memoryPressureEnabled)
            .putFloat("mem_pressure_warning_pct", s.memPressureWarningPct)
            .putFloat("mem_pressure_critical_pct", s.memPressureCriticalPct)
            .putBoolean("anr_enabled", s.anrEnabled)
            .putLong("anr_threshold_ms", s.anrThresholdMs)
            .putBoolean("slow_start_enabled", s.slowStartEnabled)
            .putLong("slow_start_cold_ms", s.slowStartColdThresholdMs)
            .putLong("slow_start_warm_ms", s.slowStartWarmThresholdMs)
            .putBoolean("show_jank", s.showJank)
            .putBoolean("show_gc", s.showGc)
            .putBoolean("show_thermal", s.showThermal)
            .putBoolean("jank_enabled", s.jankEnabled)
            .putBoolean("gc_enabled", s.gcEnabled)
            .putBoolean("thermal_enabled", s.thermalEnabled)
            .apply()
    }

    private val _settings = MutableStateFlow(loadSettings())
    actual val settings: StateFlow<KamperUiSettings> = _settings.asStateFlow()

    // ── Recording ─────────────────────────────────────────────────────────────

    private val recordingBuffer = ArrayDeque<RecordedSample>()
    private val _isRecording = MutableStateFlow(false)
    actual val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingSampleCount = MutableStateFlow(0)
    actual val recordingSampleCount: StateFlow<Int> = _recordingSampleCount.asStateFlow()

    private fun nowNs(): Long = android.os.SystemClock.elapsedRealtimeNanos()

    private fun record(trackId: Int, value: Double) {
        if (!_isRecording.value) return
        if (recordingBuffer.size >= MAX_RECORDING_SAMPLES) recordingBuffer.removeFirst()
        recordingBuffer.addLast(RecordedSample(nowNs(), trackId, value))
        _recordingSampleCount.value = recordingBuffer.size
    }

    actual fun startRecording() {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = true
    }

    actual fun stopRecording() {
        _isRecording.value = false
    }

    actual fun exportTrace(): ByteArray = PerfettoExporter.export(recordingBuffer.toList())

    actual fun clearRecording() {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = false
    }

    // ── Engine and state ──────────────────────────────────────────────────────

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

    // ── Module references (for reinstall support) ─────────────────────────────

    private var cpuModule: PerformanceModule<CpuConfig, CpuInfo>? = null
    private var memModule: PerformanceModule<MemoryConfig, MemoryInfo>? = null
    private var netModule: PerformanceModule<NetworkConfig, NetworkInfo>? = null
    private var issuesModule: PerformanceModule<IssuesConfig, IssueInfo>? = null
    private var jankModule: PerformanceModule<JankConfig, JankInfo>? = null
    private var gcModule: PerformanceModule<GcConfig, GcInfo>? = null
    private var thermalModule: PerformanceModule<ThermalConfig, ThermalInfo>? = null

    // ── Stable listener references ────────────────────────────────────────────

    private val cpuListener: InfoListener<CpuInfo> = listener@{ info ->
        if (info == CpuInfo.INVALID) return@listener
        val v = (info.totalUseRatio * 100).toFloat()
        cpuHist.push(v)
        record(Tracks.CPU, v.toDouble())
        _state.update { s -> s.copy(cpuPercent = v, cpuHistory = cpuHist.toList()) }
    }

    private val memListener: InfoListener<MemoryInfo> = listener@{ info ->
        if (info == MemoryInfo.INVALID) return@listener
        val v = info.heapMemoryInfo.allocatedInMb
        memHist.push(v)
        record(Tracks.MEMORY, v.toDouble())
        _state.update { s -> s.copy(memoryUsedMb = v, memoryHistory = memHist.toList()) }
    }

    private val netListener: InfoListener<NetworkInfo> = listener@{ info ->
        if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@listener
        val v = info.rxSystemTotalInMb
        netHist.push(v)
        record(Tracks.NETWORK, v.toDouble())
        _state.update { s -> s.copy(downloadMbps = v, downloadHistory = netHist.toList()) }
    }

    private val issuesListener: InfoListener<IssueInfo> = listener@{ info ->
        if (info == IssueInfo.INVALID) return@listener
        issuesList.add(0, info.issue)
        if (issuesList.size > MAX_ISSUES) issuesList.removeAt(issuesList.size - 1)
        saveIssues()
        _state.update { s ->
            s.copy(issues = issuesList.toList(), unreadIssueCount = s.unreadIssueCount + 1)
        }
    }

    private val jankListener: InfoListener<JankInfo> = listener@{ info ->
        if (info == JankInfo.INVALID) return@listener
        record(Tracks.JANK, info.droppedFrames.toDouble())
        _state.update { s ->
            s.copy(jankDroppedFrames = info.droppedFrames, jankRatio = info.jankyFrameRatio)
        }
    }

    private val gcListener: InfoListener<GcInfo> = listener@{ info ->
        if (info == GcInfo.INVALID) return@listener
        record(Tracks.GC, info.gcCountDelta.toDouble())
        _state.update { s ->
            s.copy(gcCountDelta = info.gcCountDelta, gcPauseMsDelta = info.gcPauseMsDelta)
        }
    }

    private val thermalListener: InfoListener<ThermalInfo> = listener@{ info ->
        if (info == ThermalInfo.INVALID) return@listener
        record(Tracks.THERMAL, info.state.ordinal.toDouble())
        _state.update { s ->
            s.copy(thermalState = info.state, isThrottling = info.isThrottling)
        }
    }

    // ── Issues persistence ────────────────────────────────────────────────────

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

    // ── FPS (Choreographer-based on Android) ──────────────────────────────────

    private var fpsFrameCount = 0
    private var fpsWindowStartNanos = 0L
    private var fpsActive = false

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
                record(Tracks.FPS, fps.toDouble())
                _state.update { s ->
                    s.copy(
                        fps        = fps,
                        fpsPeak    = maxOf(s.fpsPeak, fps),
                        fpsLow     = if (s.fpsLow == Int.MAX_VALUE) fps else minOf(s.fpsLow, fps),
                        fpsHistory = fpsHist.toList()
                    )
                }
            }
            if (fpsActive) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private fun startFps() {
        if (fpsActive) return
        fpsActive = true
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().postFrameCallback(fpsCallback)
        }
    }

    private fun stopFps() {
        fpsActive = false
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().removeFrameCallback(fpsCallback)
        }
        fpsFrameCount = 0
        fpsWindowStartNanos = 0L
    }

    // ── Module install helpers ────────────────────────────────────────────────

    private fun installCpu(s: KamperUiSettings) {
        val mod = CpuModule { intervalInMs = s.cpuIntervalMs }
        engine.install(mod)
        engine.addInfoListener(cpuListener)
        cpuModule = mod
    }

    private fun uninstallCpu() {
        cpuModule?.let { engine.uninstall(it) }
        cpuModule = null
    }

    private fun installMemory(s: KamperUiSettings) {
        val mod = MemoryModule(appContext) { intervalInMs = s.memoryIntervalMs }
        engine.install(mod)
        engine.addInfoListener(memListener)
        memModule = mod
    }

    private fun uninstallMemory() {
        memModule?.let { engine.uninstall(it) }
        memModule = null
    }

    private fun installNetwork(s: KamperUiSettings) {
        val mod = NetworkModule { intervalInMs = s.networkIntervalMs }
        engine.install(mod)
        engine.addInfoListener(netListener)
        netModule = mod
    }

    private fun uninstallNetwork() {
        netModule?.let { engine.uninstall(it) }
        netModule = null
    }

    private fun buildIssuesModule(s: KamperUiSettings) = IssuesModule(
        context    = appContext,
        anr        = AnrConfig(
            isEnabled   = s.anrEnabled,
            thresholdMs = s.anrThresholdMs
        ),
        slowStart  = SlowStartConfig(
            isEnabled             = s.slowStartEnabled,
            coldStartThresholdMs  = s.slowStartColdThresholdMs,
            warmStartThresholdMs  = s.slowStartWarmThresholdMs
        )
    ) {
        intervalInMs    = s.issuesIntervalMs
        slowSpan        = SlowSpanConfig(
            isEnabled          = s.slowSpanEnabled,
            defaultThresholdMs = s.slowSpanThresholdMs
        )
        droppedFrames   = DroppedFramesConfig(
            isEnabled                  = s.droppedFramesEnabled,
            frameThresholdMs           = s.droppedFrameThresholdMs,
            consecutiveFramesThreshold = s.droppedFrameConsecutiveThreshold
        )
        crash           = CrashConfig(
            isEnabled              = s.crashEnabled,
            chainToPreviousHandler = false
        )
        memoryPressure  = MemoryPressureConfig(
            isEnabled                = s.memoryPressureEnabled,
            warningThresholdPercent  = s.memPressureWarningPct,
            criticalThresholdPercent = s.memPressureCriticalPct
        )
    }

    private fun installIssues(s: KamperUiSettings) {
        val mod = buildIssuesModule(s)
        engine.install(mod)
        engine.addInfoListener(issuesListener)
        issuesModule = mod
    }

    private fun uninstallIssues() {
        issuesModule?.let { engine.uninstall(it) }
        issuesModule = null
    }

    private fun installJank() {
        val mod = JankModule(appContext)
        engine.install(mod)
        engine.addInfoListener(jankListener)
        jankModule = mod
    }

    private fun uninstallJank() {
        jankModule?.let { engine.uninstall(it) }
        jankModule = null
    }

    private fun installGc() {
        val mod = GcModule
        engine.install(mod)
        engine.addInfoListener(gcListener)
        gcModule = mod
    }

    private fun uninstallGc() {
        gcModule?.let { engine.uninstall(it) }
        gcModule = null
    }

    private fun installThermal() {
        val mod = ThermalModule(appContext)
        engine.install(mod)
        engine.addInfoListener(thermalListener)
        thermalModule = mod
    }

    private fun uninstallThermal() {
        thermalModule?.let { engine.uninstall(it) }
        thermalModule = null
    }

    // ── Settings update ───────────────────────────────────────────────────────

    private fun KamperUiSettings.issuesConfigKey() =
        "$issuesIntervalMs|$slowSpanEnabled|$slowSpanThresholdMs" +
        "|$droppedFramesEnabled|$droppedFrameThresholdMs|$droppedFrameConsecutiveThreshold" +
        "|$crashEnabled|$memoryPressureEnabled|$memPressureWarningPct|$memPressureCriticalPct" +
        "|$anrEnabled|$anrThresholdMs|$slowStartEnabled|$slowStartColdThresholdMs|$slowStartWarmThresholdMs"

    actual fun updateSettings(s: KamperUiSettings) {
        val old = _settings.value
        _settings.value = s
        saveSettings(s)

        val cpuConfigChanged    = old.cpuIntervalMs != s.cpuIntervalMs
        val memConfigChanged    = old.memoryIntervalMs != s.memoryIntervalMs
        val netConfigChanged    = old.networkIntervalMs != s.networkIntervalMs
        val issuesConfigChanged = old.issuesConfigKey() != s.issuesConfigKey()

        when {
            !old.cpuEnabled && s.cpuEnabled     -> installCpu(s)
            old.cpuEnabled && !s.cpuEnabled     -> uninstallCpu()
            s.cpuEnabled && cpuConfigChanged    -> { uninstallCpu(); installCpu(s) }
        }
        when {
            !old.fpsEnabled && s.fpsEnabled     -> startFps()
            old.fpsEnabled && !s.fpsEnabled     -> stopFps()
        }
        when {
            !old.memoryEnabled && s.memoryEnabled   -> installMemory(s)
            old.memoryEnabled && !s.memoryEnabled   -> uninstallMemory()
            s.memoryEnabled && memConfigChanged     -> { uninstallMemory(); installMemory(s) }
        }
        when {
            !old.networkEnabled && s.networkEnabled -> installNetwork(s)
            old.networkEnabled && !s.networkEnabled -> uninstallNetwork()
            s.networkEnabled && netConfigChanged    -> { uninstallNetwork(); installNetwork(s) }
        }
        when {
            !old.issuesEnabled && s.issuesEnabled   -> installIssues(s)
            old.issuesEnabled && !s.issuesEnabled   -> uninstallIssues()
            s.issuesEnabled && issuesConfigChanged  -> { uninstallIssues(); installIssues(s) }
        }
        when {
            !old.jankEnabled && s.jankEnabled       -> installJank()
            old.jankEnabled && !s.jankEnabled       -> uninstallJank()
        }
        when {
            !old.gcEnabled && s.gcEnabled           -> installGc()
            old.gcEnabled && !s.gcEnabled           -> uninstallGc()
        }
        when {
            !old.thermalEnabled && s.thermalEnabled -> installThermal()
            old.thermalEnabled && !s.thermalEnabled -> uninstallThermal()
        }

        if (_state.value.engineRunning) engine.start()
    }

    // ── Engine controls ───────────────────────────────────────────────────────

    actual fun startEngine() {
        engine.start()
        if (_settings.value.fpsEnabled) startFps()
        _state.update { it.copy(engineRunning = true) }
    }

    actual fun stopEngine() {
        engine.stop()
        stopFps()
        _state.update { it.copy(engineRunning = false) }
    }

    actual fun restartEngine() {
        stopEngine()
        startEngine()
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    init {
        if (issuesList.isNotEmpty()) {
            _state.update { it.copy(issues = issuesList.toList()) }
        }
        val s = _settings.value
        if (s.cpuEnabled) installCpu(s)
        if (s.memoryEnabled) installMemory(s)
        if (s.networkEnabled) installNetwork(s)
        if (s.issuesEnabled) installIssues(s)
        if (s.jankEnabled) installJank()
        if (s.gcEnabled) installGc()
        if (s.thermalEnabled) installThermal()
        engine.start()
        if (s.fpsEnabled) startFps()
    }

    actual fun clear() {
        stopFps()
        clearRecording()
        engine.clear()
        cpuModule = null
        memModule = null
        netModule = null
        issuesModule = null
        jankModule = null
        gcModule = null
        thermalModule = null
    }
}
