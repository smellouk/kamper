package com.smellouk.konitor.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.smellouk.konitor.Engine
import com.smellouk.konitor.api.InfoListener
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.cpu.CpuConfig
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.CpuModule
import com.smellouk.konitor.gc.GcConfig
import com.smellouk.konitor.gc.GcInfo
import com.smellouk.konitor.gc.GcModule
import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.CrashConfig
import com.smellouk.konitor.issues.DroppedFramesConfig
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssuesConfig
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.issues.MemoryPressureConfig
import com.smellouk.konitor.issues.SlowSpanConfig
import com.smellouk.konitor.issues.SlowStartConfig
import com.smellouk.konitor.jank.JankConfig
import com.smellouk.konitor.jank.JankInfo
import com.smellouk.konitor.jank.JankModule
import com.smellouk.konitor.memory.MemoryConfig
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryModule
import com.smellouk.konitor.network.NetworkConfig
import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.NetworkModule
import com.smellouk.konitor.thermal.ThermalConfig
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalModule
import com.smellouk.konitor.gpu.GpuConfig
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.GpuModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

private const val HISTORY_SIZE = 60
private const val MAX_ISSUES = 100
private const val PREF_ISSUES = "konitor_issues_list"
private const val PCT_SCALE = 100f
private const val SUPPORT_PROBE_DELAY_MS = 1_500L

@Suppress("TooManyFunctions")
internal class ModuleLifecycleManager(
    private val appContext: Application,
    private val state: MutableStateFlow<KonitorUiState>,
    private val recordingManager: RecordingManager,
    private val preferencesStore: PreferencesStore
) {
    private val engine = Engine()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Tracks actual engine running state separately from UI StateFlow to avoid
    // TOCTOU races in applySettings() (WR-03).
    @Volatile private var engineRunning = false

    private val cpuHist = ArrayDeque<Float>()
    private val fpsHist = ArrayDeque<Float>()
    private val memHist = ArrayDeque<Float>()
    private val netHist = ArrayDeque<Float>()
    private val gpuHist = ArrayDeque<Float>()

    private fun ArrayDeque<Float>.push(v: Float) {
        if (size >= HISTORY_SIZE) removeFirst()
        addLast(v)
    }

    // ── Module references ─────────────────────────────────────────────────────

    private var cpuModule: PerformanceModule<CpuConfig, CpuInfo>? = null
    private var memModule: PerformanceModule<MemoryConfig, MemoryInfo>? = null
    private var netModule: PerformanceModule<NetworkConfig, NetworkInfo>? = null
    private var issuesModule: PerformanceModule<IssuesConfig, IssueInfo>? = null
    private var jankModule: PerformanceModule<JankConfig, JankInfo>? = null
    private var gcModule: PerformanceModule<GcConfig, GcInfo>? = null
    private var thermalModule: PerformanceModule<ThermalConfig, ThermalInfo>? = null
    private var gpuModule: PerformanceModule<GpuConfig, GpuInfo>? = null

    // ── Stable listener references ────────────────────────────────────────────

    private val cpuListener: InfoListener<CpuInfo> = listener@{ info ->
        if (info == CpuInfo.INVALID) return@listener
        if (info == CpuInfo.UNSUPPORTED) {
            state.update { s -> s.copy(cpuUnsupported = true) }
            return@listener
        }
        val v = (info.totalUseRatio * 100).toFloat()
        cpuHist.push(v)
        recordingManager.record(Tracks.CPU, v.toDouble())
        state.update { s ->
            s.copy(
                cpuPercent = v,
                cpuHistory = cpuHist.toList(),
                cpuUnsupported = false
            )
        }
    }

    private val memListener: InfoListener<MemoryInfo> = listener@{ info ->
        if (info == MemoryInfo.INVALID) return@listener
        val v = info.heapMemoryInfo.allocatedInMb
        memHist.push(v)
        recordingManager.record(Tracks.MEMORY, v.toDouble())
        state.update { s -> s.copy(memoryUsedMb = v, memoryHistory = memHist.toList()) }
    }

    private val netListener: InfoListener<NetworkInfo> = listener@{ info ->
        if (info == NetworkInfo.INVALID) return@listener
        if (info == NetworkInfo.NOT_SUPPORTED) {
            state.update { s -> s.copy(networkUnsupported = true) }
            return@listener
        }
        val v = info.rxSystemTotalInMb
        netHist.push(v)
        recordingManager.record(Tracks.NETWORK, v.toDouble())
        state.update { s -> s.copy(downloadMbps = v, downloadHistory = netHist.toList(), networkUnsupported = false) }
    }

    private val issueRecords: MutableList<IssueRecord> = mutableListOf()

    private val issuesListener: InfoListener<IssueInfo> = listener@{ info ->
        if (info == IssueInfo.INVALID) return@listener
        val record = IssueRecord(info.issue, android.os.SystemClock.elapsedRealtimeNanos())
        synchronized(issuesList) {
            issueRecords.add(0, record)
            if (issueRecords.size > MAX_ISSUES) issueRecords.removeAt(issueRecords.size - 1)
            issuesList.add(0, info.issue)
            if (issuesList.size > MAX_ISSUES) issuesList.removeAt(issuesList.size - 1)
            saveIssues()
        }
        state.update { s ->
            s.copy(issues = issuesList.toList(), unreadIssueCount = s.unreadIssueCount + 1)
        }
    }

    private val jankListener: InfoListener<JankInfo> = listener@{ info ->
        if (info == JankInfo.INVALID) return@listener
        if (info == JankInfo.UNSUPPORTED) {
            state.update { s -> s.copy(jankUnsupported = true) }
            return@listener
        }
        recordingManager.record(Tracks.JANK, info.droppedFrames.toDouble())
        state.update { s ->
            s.copy(jankDroppedFrames = info.droppedFrames, jankRatio = info.jankyFrameRatio, jankUnsupported = false)
        }
    }

    private val gcListener: InfoListener<GcInfo> = listener@{ info ->
        if (info == GcInfo.INVALID) return@listener
        if (info == GcInfo.UNSUPPORTED) {
            state.update { s -> s.copy(gcUnsupported = true) }
            return@listener
        }
        recordingManager.record(Tracks.GC, info.gcCountDelta.toDouble())
        state.update { s ->
            s.copy(gcCountDelta = info.gcCountDelta, gcPauseMsDelta = info.gcPauseMsDelta)
        }
    }

    private val thermalListener: InfoListener<ThermalInfo> = listener@{ info ->
        if (info == ThermalInfo.INVALID) return@listener
        if (info == ThermalInfo.UNSUPPORTED) {
            state.update { s -> s.copy(thermalUnsupported = true) }
            return@listener
        }
        recordingManager.record(Tracks.THERMAL, info.state.ordinal.toDouble())
        state.update { s ->
            s.copy(
                thermalState = info.state,
                isThrottling = info.isThrottling,
                thermalUnsupported = false
            )
        }
    }

    private val gpuListener: InfoListener<GpuInfo> = listener@{ info ->
        if (info == GpuInfo.INVALID) return@listener
        if (info == GpuInfo.UNSUPPORTED) {
            state.update { s -> s.copy(gpuUnsupported = true) }
            return@listener
        }
        val utilFloat = info.utilization.toFloat()
        // When utilization is unknown (-1.0), fall back to cur_freq as the activity signal.
        // The Sparkline normalises against data.max(), so raw KHz works for relative trends.
        val histValue: Float = when {
            utilFloat >= 0f -> utilFloat
            info.curFreqKhz >= 0L && info.maxFreqKhz > 0L ->
                (info.curFreqKhz.toFloat() / info.maxFreqKhz.toFloat()) * PCT_SCALE
            info.curFreqKhz >= 0L -> info.curFreqKhz.toFloat()
            else -> -1f
        }
        if (histValue >= 0f) gpuHist.push(histValue)
        if (histValue >= 0f) recordingManager.record(Tracks.GPU, histValue.toDouble())
        state.update { s ->
            s.copy(
                gpuUtilization   = utilFloat,
                gpuUsedMemoryMb  = info.usedMemoryMb.toFloat(),
                gpuTotalMemoryMb = info.totalMemoryMb.toFloat(),
                gpuHistory       = gpuHist.toList(),
                gpuUnsupported   = false
            )
        }
    }

    // ── Issues persistence ────────────────────────────────────────────────────

    private val issuesList: MutableList<com.smellouk.konitor.issues.Issue> = loadPersistedIssues()

    private fun loadPersistedIssues(): MutableList<com.smellouk.konitor.issues.Issue> {
        val raw = preferencesStore.getString(PREF_ISSUES, "")
        if (raw.isEmpty()) return mutableListOf()
        // Records are separated by Group Separator (0x1D), which pctEncode() will escape
        // if it ever appears inside a field value — making the format self-consistent.
        return raw.split('').mapNotNull { it.deserializeIssue() }.toMutableList()
    }

    private fun saveIssues() {
        // Use Group Separator (0x1D) instead of newline so that any newline inside
        // field values (e.g. stack traces) cannot corrupt the record boundaries.
        preferencesStore.putString(PREF_ISSUES, issuesList.joinToString("") { it.serialize() })
    }

    fun clearIssues() {
        synchronized(issuesList) {
            issueRecords.clear()
            issuesList.clear()
            saveIssues()
        }
        state.update { it.copy(issues = emptyList(), unreadIssueCount = 0) }
    }

    fun snapshotIssueRecords(): List<IssueRecord> = synchronized(issuesList) { issueRecords.toList() }

    // ── FPS (Choreographer-based on Android) ──────────────────────────────────

    @Volatile private var fpsFrameCount = 0
    @Volatile private var fpsWindowStartNanos = 0L
    @Volatile private var fpsActive = false

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
                recordingManager.record(Tracks.FPS, fps.toDouble())
                state.update { s ->
                    s.copy(
                        fps     = fps,
                        fpsPeak = maxOf(s.fpsPeak, fps),
                        fpsLow  = if (s.fpsLow == Int.MAX_VALUE) fps else minOf(s.fpsLow, fps),
                        fpsHistory = fpsHist.toList()
                    )
                }
            }
            if (fpsActive) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun startFps() {
        if (fpsActive) return
        fpsActive = true
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().postFrameCallback(fpsCallback)
        }
    }

    fun stopFps() {
        fpsActive = false
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().removeFrameCallback(fpsCallback)
            // Reset counters on the Choreographer thread to avoid races with doFrame
            fpsFrameCount = 0
            fpsWindowStartNanos = 0L
        }
        // Reset derived state so stale peak/low don't bleed into the next measurement window
        state.update { it.copy(fpsPeak = 0, fpsLow = Int.MAX_VALUE, fpsHistory = emptyList()) }
    }

    // ── Module install helpers ────────────────────────────────────────────────

    private fun installCpu(s: KonitorUiSettings) {
        val mod = CpuModule { intervalInMs = s.cpuIntervalMs }
        engine.install(mod)
        engine.addInfoListener(cpuListener)
        cpuModule = mod
    }

    private fun uninstallCpu() {
        cpuModule?.let { engine.uninstall(it) }
        cpuModule = null
    }

    private fun installMemory(s: KonitorUiSettings) {
        val mod = MemoryModule(appContext) { intervalInMs = s.memoryIntervalMs }
        engine.install(mod)
        engine.addInfoListener(memListener)
        memModule = mod
    }

    private fun uninstallMemory() {
        memModule?.let { engine.uninstall(it) }
        memModule = null
    }

    private fun installNetwork(s: KonitorUiSettings) {
        val mod = NetworkModule { intervalInMs = s.networkIntervalMs }
        engine.install(mod)
        engine.addInfoListener(netListener)
        netModule = mod
    }

    private fun uninstallNetwork() {
        netModule?.let { engine.uninstall(it) }
        netModule = null
    }

    private fun buildIssuesModule(s: KonitorUiSettings) = IssuesModule(
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

    private fun installIssues(s: KonitorUiSettings) {
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

    private fun installGpu() {
        val mod = GpuModule
        engine.install(mod)
        engine.addInfoListener(gpuListener)
        gpuModule = mod
    }

    private fun uninstallGpu() {
        gpuModule?.let { engine.uninstall(it) }
        gpuModule = null
    }

    // ── issuesConfigKey ───────────────────────────────────────────────────────

    private fun KonitorUiSettings.issuesConfigKey() =
        "$issuesIntervalMs|$slowSpanEnabled|$slowSpanThresholdMs" +
        "|$droppedFramesEnabled|$droppedFrameThresholdMs|$droppedFrameConsecutiveThreshold" +
        "|$crashEnabled|$memoryPressureEnabled|$memPressureWarningPct|$memPressureCriticalPct" +
        "|$anrEnabled|$anrThresholdMs|$slowStartEnabled|$slowStartColdThresholdMs|$slowStartWarmThresholdMs"

    // ── Settings apply (module reinstall logic) ───────────────────────────────

    fun applySettings(old: KonitorUiSettings, normalized: KonitorUiSettings) {
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
            !old.fpsEnabled && normalized.fpsEnabled     -> startFps()
            old.fpsEnabled && !normalized.fpsEnabled     -> stopFps()
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
        when {
            !old.gpuEnabled && normalized.gpuEnabled -> installGpu()
            old.gpuEnabled && !normalized.gpuEnabled -> uninstallGpu()
        }

        if (engineRunning) {
            engine.stop()   // stop currently running instances cleanly
            engine.start()  // restart with the updated module set
        }
    }

    // ── Engine controls ───────────────────────────────────────────────────────

    fun startEngine() {
        engine.start()
        engineRunning = true
        state.update { it.copy(engineRunning = true) }
    }

    fun stopEngine() {
        engine.stop()
        engineRunning = false
        state.update { it.copy(engineRunning = false) }
    }

    fun restartEngine() {
        stopEngine()
        startEngine()
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    fun initialise(s: KonitorUiSettings) {
        if (issuesList.isNotEmpty()) {
            state.update { it.copy(issues = issuesList.toList()) }
        }
        if (s.cpuEnabled) installCpu(s)
        if (s.memoryEnabled) installMemory(s)
        if (s.networkEnabled) installNetwork(s)
        if (s.issuesEnabled) installIssues(s)
        if (s.jankEnabled) installJank()
        if (s.gcEnabled) installGc()
        if (s.thermalEnabled) installThermal()
        if (s.gpuEnabled) installGpu()

        // Probe potentially-unsupported modules so the "not supported" label
        // appears before the user manually enables them.
        val probeCpu     = !s.cpuEnabled
        val probeNetwork = !s.networkEnabled
        val probeJank    = !s.jankEnabled
        val probeThermal = !s.thermalEnabled
        val probeGpu     = !s.gpuEnabled
        if (probeCpu)     installCpu(s)
        if (probeNetwork) installNetwork(s)
        if (probeJank)    installJank()
        if (probeThermal) installThermal()
        if (probeGpu)     installGpu()

        engine.start()
        engineRunning = true
        if (s.fpsEnabled) startFps()
        state.update { it.copy(engineRunning = true) }

        // After one watcher tick, remove probe modules that aren't user-enabled.
        val anyProbe = probeCpu || probeNetwork || probeJank || probeThermal || probeGpu
        if (anyProbe) {
            scope.launch {
                delay(SUPPORT_PROBE_DELAY_MS)
                if (probeCpu)     uninstallCpu()
                if (probeNetwork) uninstallNetwork()
                if (probeJank)    uninstallJank()
                if (probeThermal) uninstallThermal()
                if (probeGpu)     uninstallGpu()
                if (engineRunning) { engine.stop(); engine.start() }
            }
        }
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    fun clear() {
        scope.cancel()
        stopFps()
        engine.clear()
        cpuModule = null
        memModule = null
        netModule = null
        issuesModule = null
        jankModule = null
        gcModule = null
        thermalModule = null
        gpuModule = null
    }
}
