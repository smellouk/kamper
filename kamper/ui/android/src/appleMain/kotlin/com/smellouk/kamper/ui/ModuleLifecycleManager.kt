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
import com.smellouk.kamper.gc.GcConfig
import com.smellouk.kamper.gc.GcInfo
import com.smellouk.kamper.gc.GcModule
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.CrashConfig
import com.smellouk.kamper.issues.DroppedFramesConfig
import com.smellouk.kamper.issues.IssueInfo
import com.smellouk.kamper.issues.IssuesConfig
import com.smellouk.kamper.issues.IssuesModule
import com.smellouk.kamper.issues.MemoryPressureConfig
import com.smellouk.kamper.issues.SlowSpanConfig
import com.smellouk.kamper.issues.SlowStartConfig
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
import kotlinx.coroutines.flow.update

private const val HISTORY_SIZE = 60
private const val MAX_ISSUES = 100
private const val PREF_ISSUES = "kamper_issues_list"

@Suppress("TooManyFunctions")
internal class ModuleLifecycleManager(
    private val state: MutableStateFlow<KamperUiState>,
    private val preferencesStore: PreferencesStore
) {
    private val engine = Engine()

    private val cpuHist = ArrayDeque<Float>()
    private val fpsHist = ArrayDeque<Float>()
    private val memHist = ArrayDeque<Float>()
    private val netHist = ArrayDeque<Float>()

    private fun ArrayDeque<Float>.push(v: Float) {
        if (size >= HISTORY_SIZE) removeFirst()
        addLast(v)
    }

    private var cpuModule: PerformanceModule<CpuConfig, CpuInfo>? = null
    private var fpsModule: PerformanceModule<FpsConfig, FpsInfo>? = null
    private var memModule: PerformanceModule<MemoryConfig, MemoryInfo>? = null
    private var netModule: PerformanceModule<NetworkConfig, NetworkInfo>? = null
    private var issuesModule: PerformanceModule<IssuesConfig, IssueInfo>? = null
    private var jankModule: PerformanceModule<JankConfig, JankInfo>? = null
    private var gcModule: PerformanceModule<GcConfig, GcInfo>? = null
    private var thermalModule: PerformanceModule<ThermalConfig, ThermalInfo>? = null

    private val cpuListener: InfoListener<CpuInfo> = listener@{ info ->
        if (info == CpuInfo.INVALID) return@listener
        val v = (info.totalUseRatio * 100).toFloat()
        cpuHist.push(v)
        state.update { s -> s.copy(cpuPercent = v, cpuHistory = cpuHist.toList()) }
    }

    private val fpsListener: InfoListener<FpsInfo> = listener@{ info ->
        if (info == FpsInfo.INVALID) return@listener
        val v = info.fps
        fpsHist.push(v.toFloat())
        state.update { s ->
            s.copy(
                fps     = v,
                fpsPeak = maxOf(s.fpsPeak, v),
                fpsLow  = if (s.fpsLow == Int.MAX_VALUE) v else minOf(s.fpsLow, v),
                fpsHistory = fpsHist.toList()
            )
        }
    }

    private val memListener: InfoListener<MemoryInfo> = listener@{ info ->
        if (info == MemoryInfo.INVALID) return@listener
        val v = info.heapMemoryInfo.allocatedInMb
        memHist.push(v)
        state.update { s -> s.copy(memoryUsedMb = v, memoryHistory = memHist.toList()) }
    }

    private val netListener: InfoListener<NetworkInfo> = listener@{ info ->
        if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@listener
        val v = info.rxSystemTotalInMb
        netHist.push(v)
        state.update { s -> s.copy(downloadMbps = v, downloadHistory = netHist.toList()) }
    }

    private val issuesListener: InfoListener<IssueInfo> = listener@{ info ->
        if (info == IssueInfo.INVALID) return@listener
        val updated = (listOf(info.issue) + issuesList).take(MAX_ISSUES)
        issuesList = updated
        saveIssues()
        state.update { s ->
            s.copy(issues = updated, unreadIssueCount = s.unreadIssueCount + 1)
        }
    }

    private val jankListener: InfoListener<JankInfo> = listener@{ info ->
        if (info == JankInfo.INVALID) return@listener
        state.update { s ->
            s.copy(jankDroppedFrames = info.droppedFrames, jankRatio = info.jankyFrameRatio)
        }
    }

    private val gcListener: InfoListener<GcInfo> = listener@{ info ->
        if (info == GcInfo.INVALID) return@listener
        state.update { s ->
            s.copy(gcCountDelta = info.gcCountDelta, gcPauseMsDelta = info.gcPauseMsDelta)
        }
    }

    private val thermalListener: InfoListener<ThermalInfo> = listener@{ info ->
        if (info == ThermalInfo.INVALID) return@listener
        state.update { s ->
            s.copy(thermalState = info.state, isThrottling = info.isThrottling)
        }
    }

    private var issuesList: List<com.smellouk.kamper.issues.Issue> = loadPersistedIssues()

    private fun loadPersistedIssues(): List<com.smellouk.kamper.issues.Issue> {
        val raw = preferencesStore.getString(PREF_ISSUES, "")
        if (raw.isEmpty()) return emptyList()
        return raw.lines().mapNotNull { it.deserializeIssue() }
    }

    private fun saveIssues() {
        preferencesStore.putString(PREF_ISSUES, issuesList.joinToString("\n") { it.serialize() })
    }

    fun clearIssues() {
        issuesList = emptyList()
        saveIssues()
        state.update { it.copy(issues = emptyList(), unreadIssueCount = 0) }
    }

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

    private fun KamperUiSettings.issuesConfigKey() =
        "$issuesIntervalMs|$slowSpanEnabled|$slowSpanThresholdMs" +
        "|$droppedFramesEnabled|$droppedFrameThresholdMs|$droppedFrameConsecutiveThreshold" +
        "|$crashEnabled|$memoryPressureEnabled|$memPressureWarningPct|$memPressureCriticalPct" +
        "|$anrEnabled|$anrThresholdMs|$slowStartEnabled|$slowStartColdThresholdMs|$slowStartWarmThresholdMs"

    fun applySettings(old: KamperUiSettings, normalized: KamperUiSettings) {
        val cpuConfigChanged    = old.cpuIntervalMs != normalized.cpuIntervalMs
        val memConfigChanged    = old.memoryIntervalMs != normalized.memoryIntervalMs
        val netConfigChanged    = old.networkIntervalMs != normalized.networkIntervalMs
        val issuesConfigChanged = old.issuesConfigKey() != normalized.issuesConfigKey()

        when {
            !old.cpuEnabled && normalized.cpuEnabled     -> installCpu(normalized)
            old.cpuEnabled && !normalized.cpuEnabled     -> uninstallCpu()
            normalized.cpuEnabled && cpuConfigChanged    -> { uninstallCpu(); installCpu(normalized) }
        }
        when {
            !old.fpsEnabled && normalized.fpsEnabled     -> installFps()
            old.fpsEnabled && !normalized.fpsEnabled     -> uninstallFps()
        }
        when {
            !old.memoryEnabled && normalized.memoryEnabled   -> installMemory(normalized)
            old.memoryEnabled && !normalized.memoryEnabled   -> uninstallMemory()
            normalized.memoryEnabled && memConfigChanged     -> { uninstallMemory(); installMemory(normalized) }
        }
        when {
            !old.networkEnabled && normalized.networkEnabled -> installNetwork(normalized)
            old.networkEnabled && !normalized.networkEnabled -> uninstallNetwork()
            normalized.networkEnabled && netConfigChanged    -> { uninstallNetwork(); installNetwork(normalized) }
        }
        when {
            !old.issuesEnabled && normalized.issuesEnabled   -> installIssues(normalized)
            old.issuesEnabled && !normalized.issuesEnabled   -> uninstallIssues()
            normalized.issuesEnabled && issuesConfigChanged  -> { uninstallIssues(); installIssues(normalized) }
        }
        when {
            !old.jankEnabled && normalized.jankEnabled       -> installJank()
            old.jankEnabled && !normalized.jankEnabled       -> uninstallJank()
        }
        when {
            !old.gcEnabled && normalized.gcEnabled           -> installGc()
            old.gcEnabled && !normalized.gcEnabled           -> uninstallGc()
        }
        when {
            !old.thermalEnabled && normalized.thermalEnabled -> installThermal()
            old.thermalEnabled && !normalized.thermalEnabled -> uninstallThermal()
        }

        if (state.value.engineRunning) {
            engine.stop()   // stop currently running instances cleanly
            engine.start()  // restart with the updated module set
        }
    }

    fun startEngine() {
        engine.start()
        state.update { it.copy(engineRunning = true) }
    }

    fun stopEngine() {
        engine.stop()
        state.update { it.copy(engineRunning = false) }
    }

    fun restartEngine() {
        stopEngine()
        startEngine()
    }

    fun initialise(s: KamperUiSettings) {
        if (issuesList.isNotEmpty()) {
            state.update { it.copy(issues = issuesList.toList()) }
        }
        if (s.cpuEnabled) installCpu(s)
        if (s.fpsEnabled) installFps()
        if (s.memoryEnabled) installMemory(s)
        if (s.networkEnabled) installNetwork(s)
        if (s.issuesEnabled) installIssues(s)
        if (s.jankEnabled) installJank()
        if (s.gcEnabled) installGc()
        if (s.thermalEnabled) installThermal()
        engine.start()
        state.update { it.copy(engineRunning = true) }
    }

    fun clear() {
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
