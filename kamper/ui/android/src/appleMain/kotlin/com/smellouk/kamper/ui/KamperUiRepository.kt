package com.smellouk.kamper.ui

import com.smellouk.kamper.Engine
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.cpu.CpuConfig
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.CpuModule
import com.smellouk.kamper.fps.FpsConfig
import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.FpsModule
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
import platform.Foundation.NSUserDefaults

private const val HISTORY_SIZE = 60
private const val MAX_ISSUES = 100
private const val PREF_ISSUES = "kamper_issues_list"

internal actual class KamperUiRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    // ── Settings persistence ──────────────────────────────────────────────────

    private fun bool(key: String, default: Boolean = true) =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    private fun long(key: String, default: Long) =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toLong() else default

    private fun float(key: String, default: Float) =
        if (defaults.objectForKey(key) != null) defaults.floatForKey(key) else default

    private fun int(key: String, default: Int) =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default

    private fun loadSettings() = KamperUiSettings(
        showCpu                           = bool("show_cpu"),
        showFps                           = bool("show_fps"),
        showMemory                        = bool("show_memory"),
        showNetwork                       = bool("show_network"),
        showIssues                        = bool("show_issues"),
        cpuEnabled                        = bool("cpu_enabled"),
        fpsEnabled                        = bool("fps_enabled"),
        memoryEnabled                     = bool("memory_enabled"),
        networkEnabled                    = bool("network_enabled"),
        issuesEnabled                     = bool("issues_enabled"),
        cpuIntervalMs                     = long("cpu_interval_ms", 1_000L),
        memoryIntervalMs                  = long("memory_interval_ms", 1_000L),
        networkIntervalMs                 = long("network_interval_ms", 1_000L),
        issuesIntervalMs                  = long("issues_interval_ms", 1_000L),
        slowSpanEnabled                   = bool("slow_span_enabled"),
        slowSpanThresholdMs               = long("slow_span_threshold_ms", 1_000L),
        droppedFramesEnabled              = bool("dropped_frames_enabled"),
        droppedFrameThresholdMs           = long("dropped_frame_threshold_ms", 32L),
        droppedFrameConsecutiveThreshold  = int("dropped_frame_consecutive", 3),
        crashEnabled                      = bool("crash_enabled"),
        memoryPressureEnabled             = bool("mem_pressure_enabled"),
        memPressureWarningPct             = float("mem_pressure_warning_pct", 0.80f),
        memPressureCriticalPct            = float("mem_pressure_critical_pct", 0.95f),
        anrEnabled                        = bool("anr_enabled"),
        anrThresholdMs                    = long("anr_threshold_ms", 5_000L),
        slowStartEnabled                  = bool("slow_start_enabled"),
        slowStartColdThresholdMs          = long("slow_start_cold_ms", 2_000L),
        slowStartWarmThresholdMs          = long("slow_start_warm_ms", 800L),
        showJank                          = bool("show_jank"),
        showGc                            = bool("show_gc"),
        showThermal                       = bool("show_thermal"),
        jankEnabled                       = bool("jank_enabled"),
        gcEnabled                         = bool("gc_enabled"),
        thermalEnabled                    = bool("thermal_enabled")
    )

    private fun saveSettings(s: KamperUiSettings) {
        defaults.apply {
            setBool(s.showCpu, "show_cpu")
            setBool(s.showFps, "show_fps")
            setBool(s.showMemory, "show_memory")
            setBool(s.showNetwork, "show_network")
            setBool(s.showIssues, "show_issues")
            setBool(s.cpuEnabled, "cpu_enabled")
            setBool(s.fpsEnabled, "fps_enabled")
            setBool(s.memoryEnabled, "memory_enabled")
            setBool(s.networkEnabled, "network_enabled")
            setBool(s.issuesEnabled, "issues_enabled")
            setInteger(s.cpuIntervalMs, "cpu_interval_ms")
            setInteger(s.memoryIntervalMs, "memory_interval_ms")
            setInteger(s.networkIntervalMs, "network_interval_ms")
            setInteger(s.issuesIntervalMs, "issues_interval_ms")
            setBool(s.slowSpanEnabled, "slow_span_enabled")
            setInteger(s.slowSpanThresholdMs, "slow_span_threshold_ms")
            setBool(s.droppedFramesEnabled, "dropped_frames_enabled")
            setInteger(s.droppedFrameThresholdMs, "dropped_frame_threshold_ms")
            setInteger(s.droppedFrameConsecutiveThreshold.toLong(), "dropped_frame_consecutive")
            setBool(s.crashEnabled, "crash_enabled")
            setBool(s.memoryPressureEnabled, "mem_pressure_enabled")
            setFloat(s.memPressureWarningPct, "mem_pressure_warning_pct")
            setFloat(s.memPressureCriticalPct, "mem_pressure_critical_pct")
            setBool(s.anrEnabled, "anr_enabled")
            setInteger(s.anrThresholdMs, "anr_threshold_ms")
            setBool(s.slowStartEnabled, "slow_start_enabled")
            setInteger(s.slowStartColdThresholdMs, "slow_start_cold_ms")
            setInteger(s.slowStartWarmThresholdMs, "slow_start_warm_ms")
            setBool(s.showJank, "show_jank")
            setBool(s.showGc, "show_gc")
            setBool(s.showThermal, "show_thermal")
            setBool(s.jankEnabled, "jank_enabled")
            setBool(s.gcEnabled, "gc_enabled")
            setBool(s.thermalEnabled, "thermal_enabled")
        }
    }

    private val _settings = MutableStateFlow(loadSettings())
    actual val settings: StateFlow<KamperUiSettings> = _settings.asStateFlow()

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

    // ── Module references ─────────────────────────────────────────────────────

    private var cpuModule: PerformanceModule<CpuConfig, CpuInfo>? = null
    private var fpsModule: PerformanceModule<FpsConfig, FpsInfo>? = null
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
        _state.update { s -> s.copy(cpuPercent = v, cpuHistory = cpuHist.toList()) }
    }

    private val fpsListener: InfoListener<FpsInfo> = listener@{ info ->
        if (info == FpsInfo.INVALID) return@listener
        val v = info.fps
        fpsHist.push(v.toFloat())
        _state.update { s ->
            s.copy(
                fps        = v,
                fpsPeak    = maxOf(s.fpsPeak, v),
                fpsLow     = if (s.fpsLow == Int.MAX_VALUE) v else minOf(s.fpsLow, v),
                fpsHistory = fpsHist.toList()
            )
        }
    }

    private val memListener: InfoListener<MemoryInfo> = listener@{ info ->
        if (info == MemoryInfo.INVALID) return@listener
        val v = info.heapMemoryInfo.allocatedInMb
        memHist.push(v)
        _state.update { s -> s.copy(memoryUsedMb = v, memoryHistory = memHist.toList()) }
    }

    private val netListener: InfoListener<NetworkInfo> = listener@{ info ->
        if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@listener
        val v = info.rxSystemTotalInMb
        netHist.push(v)
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
        _state.update { s ->
            s.copy(jankDroppedFrames = info.droppedFrames, jankRatio = info.jankyFrameRatio)
        }
    }

    private val gcListener: InfoListener<GcInfo> = listener@{ info ->
        if (info == GcInfo.INVALID) return@listener
        _state.update { s ->
            s.copy(gcCountDelta = info.gcCountDelta, gcPauseMsDelta = info.gcPauseMsDelta)
        }
    }

    private val thermalListener: InfoListener<ThermalInfo> = listener@{ info ->
        if (info == ThermalInfo.INVALID) return@listener
        _state.update { s ->
            s.copy(thermalState = info.state, isThrottling = info.isThrottling)
        }
    }

    // ── Issues persistence ────────────────────────────────────────────────────

    private val issuesList = loadPersistedIssues()

    private fun loadPersistedIssues(): MutableList<com.smellouk.kamper.issues.Issue> {
        val raw = defaults.stringForKey(PREF_ISSUES) ?: return mutableListOf()
        return raw.lines().mapNotNull { it.deserializeIssue() }.toMutableList()
    }

    private fun saveIssues() {
        defaults.setObject(issuesList.joinToString("\n") { it.serialize() }, PREF_ISSUES)
    }

    actual fun clearIssues() {
        issuesList.clear()
        saveIssues()
        _state.update { it.copy(issues = emptyList(), unreadIssueCount = 0) }
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

    private fun installFps() {
        val mod = FpsModule
        engine.install(mod)
        engine.addInfoListener(fpsListener)
        fpsModule = mod
    }

    private fun uninstallFps() {
        fpsModule?.let { engine.uninstall(it) }
        fpsModule = null
    }

    private fun installMemory(s: KamperUiSettings) {
        val mod = MemoryModule { intervalInMs = s.memoryIntervalMs }
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
        anr       = AnrConfig(
            isEnabled   = s.anrEnabled,
            thresholdMs = s.anrThresholdMs
        ),
        slowStart = SlowStartConfig(
            isEnabled            = s.slowStartEnabled,
            coldStartThresholdMs = s.slowStartColdThresholdMs,
            warmStartThresholdMs = s.slowStartWarmThresholdMs
        )
    ) {
        intervalInMs   = s.issuesIntervalMs
        slowSpan       = SlowSpanConfig(
            isEnabled          = s.slowSpanEnabled,
            defaultThresholdMs = s.slowSpanThresholdMs
        )
        droppedFrames  = DroppedFramesConfig(
            isEnabled                  = s.droppedFramesEnabled,
            frameThresholdMs           = s.droppedFrameThresholdMs,
            consecutiveFramesThreshold = s.droppedFrameConsecutiveThreshold
        )
        crash          = CrashConfig(
            isEnabled              = s.crashEnabled,
            chainToPreviousHandler = false
        )
        memoryPressure = MemoryPressureConfig(
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
        val mod = JankModule
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
        val mod = ThermalModule
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
            !old.fpsEnabled && s.fpsEnabled     -> installFps()
            old.fpsEnabled && !s.fpsEnabled     -> uninstallFps()
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
        _state.update { it.copy(engineRunning = true) }
    }

    actual fun stopEngine() {
        engine.stop()
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
        if (s.fpsEnabled) installFps()
        if (s.memoryEnabled) installMemory(s)
        if (s.networkEnabled) installNetwork(s)
        if (s.issuesEnabled) installIssues(s)
        if (s.jankEnabled) installJank()
        if (s.gcEnabled) installGc()
        if (s.thermalEnabled) installThermal()
        engine.start()
    }

    actual fun clear() {
        engine.clear()
        cpuModule = null
        fpsModule = null
        memModule = null
        netModule = null
        issuesModule = null
        jankModule = null
        gcModule = null
        thermalModule = null
    }
}
