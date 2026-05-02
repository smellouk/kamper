package com.smellouk.kamper.ui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class SettingsRepository(
    private val store: PreferencesStore,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private val _settings = MutableStateFlow(loadSettingsSync())
    val settings: StateFlow<KamperUiSettings> = _settings.asStateFlow()

    private fun loadSettingsSync(): KamperUiSettings = runCatching {
        KamperUiSettings(
        showCpu                          = store.getBoolean("show_cpu", true),
        showFps                          = store.getBoolean("show_fps", true),
        showMemory                       = store.getBoolean("show_memory", true),
        showNetwork                      = store.getBoolean("show_network", true),
        showIssues                       = store.getBoolean("show_issues", true),
        showJank                         = store.getBoolean("show_jank", false),
        showGc                           = store.getBoolean("show_gc", false),
        showThermal                      = store.getBoolean("show_thermal", false),
        showGpu                          = store.getBoolean("show_gpu", true),
        cpuEnabled                       = store.getBoolean("cpu_enabled", true),
        fpsEnabled                       = store.getBoolean("fps_enabled", true),
        memoryEnabled                    = store.getBoolean("memory_enabled", true),
        networkEnabled                   = store.getBoolean("network_enabled", true),
        issuesEnabled                    = store.getBoolean("issues_enabled", true),
        jankEnabled                      = store.getBoolean("jank_enabled", false),
        gcEnabled                        = store.getBoolean("gc_enabled", false),
        thermalEnabled                   = store.getBoolean("thermal_enabled", false),
        gpuEnabled                       = store.getBoolean("gpu_enabled", true),
        cpuIntervalMs                    = store.getLong("cpu_interval_ms", 1_000L),
        memoryIntervalMs                 = store.getLong("memory_interval_ms", 1_000L),
        networkIntervalMs                = store.getLong("network_interval_ms", 1_000L),
        issuesIntervalMs                 = store.getLong("issues_interval_ms", 1_000L),
        slowSpanEnabled                  = store.getBoolean("slow_span_enabled", true),
        slowSpanThresholdMs              = store.getLong("slow_span_threshold_ms", 1_000L),
        droppedFramesEnabled             = store.getBoolean("dropped_frames_enabled", true),
        droppedFrameThresholdMs          = store.getLong("dropped_frame_threshold_ms", 32L),
        droppedFrameConsecutiveThreshold = store.getInt("dropped_frame_consecutive", 3),
        crashEnabled                     = store.getBoolean("crash_enabled", true),
        memoryPressureEnabled            = store.getBoolean("mem_pressure_enabled", true),
        memPressureWarningPct            = store.getFloat("mem_pressure_warning_pct", 0.80f),
        memPressureCriticalPct           = store.getFloat("mem_pressure_critical_pct", 0.95f),
        anrEnabled                       = store.getBoolean("anr_enabled", true),
        anrThresholdMs                   = store.getLong("anr_threshold_ms", 5_000L),
        slowStartEnabled                 = store.getBoolean("slow_start_enabled", true),
        slowStartColdThresholdMs         = store.getLong("slow_start_cold_ms", 2_000L),
        slowStartWarmThresholdMs         = store.getLong("slow_start_warm_ms", 800L),
        isDarkTheme                      = store.getBoolean("is_dark_theme", true)
        )
    }.getOrDefault(KamperUiSettings())

    suspend fun loadSettings(): KamperUiSettings = withContext(dispatcher) {
        loadSettingsSync().also { _settings.value = it }
    }

    fun updateSettings(s: KamperUiSettings) {
        _settings.value = s
        scope.launch { saveSettingsSync(s) }
    }

    private fun saveSettingsSync(s: KamperUiSettings) {
        store.putBoolean("show_cpu", s.showCpu)
        store.putBoolean("show_fps", s.showFps)
        store.putBoolean("show_memory", s.showMemory)
        store.putBoolean("show_network", s.showNetwork)
        store.putBoolean("show_issues", s.showIssues)
        store.putBoolean("show_jank", s.showJank)
        store.putBoolean("show_gc", s.showGc)
        store.putBoolean("show_thermal", s.showThermal)
        store.putBoolean("show_gpu", s.showGpu)
        store.putBoolean("cpu_enabled", s.cpuEnabled)
        store.putBoolean("fps_enabled", s.fpsEnabled)
        store.putBoolean("memory_enabled", s.memoryEnabled)
        store.putBoolean("network_enabled", s.networkEnabled)
        store.putBoolean("issues_enabled", s.issuesEnabled)
        store.putBoolean("jank_enabled", s.jankEnabled)
        store.putBoolean("gc_enabled", s.gcEnabled)
        store.putBoolean("thermal_enabled", s.thermalEnabled)
        store.putBoolean("gpu_enabled", s.gpuEnabled)
        store.putLong("cpu_interval_ms", s.cpuIntervalMs)
        store.putLong("memory_interval_ms", s.memoryIntervalMs)
        store.putLong("network_interval_ms", s.networkIntervalMs)
        store.putLong("issues_interval_ms", s.issuesIntervalMs)
        store.putBoolean("slow_span_enabled", s.slowSpanEnabled)
        store.putLong("slow_span_threshold_ms", s.slowSpanThresholdMs)
        store.putBoolean("dropped_frames_enabled", s.droppedFramesEnabled)
        store.putLong("dropped_frame_threshold_ms", s.droppedFrameThresholdMs)
        store.putInt("dropped_frame_consecutive", s.droppedFrameConsecutiveThreshold)
        store.putBoolean("crash_enabled", s.crashEnabled)
        store.putBoolean("mem_pressure_enabled", s.memoryPressureEnabled)
        store.putFloat("mem_pressure_warning_pct", s.memPressureWarningPct)
        store.putFloat("mem_pressure_critical_pct", s.memPressureCriticalPct)
        store.putBoolean("anr_enabled", s.anrEnabled)
        store.putLong("anr_threshold_ms", s.anrThresholdMs)
        store.putBoolean("slow_start_enabled", s.slowStartEnabled)
        store.putLong("slow_start_cold_ms", s.slowStartColdThresholdMs)
        store.putLong("slow_start_warm_ms", s.slowStartWarmThresholdMs)
        store.putBoolean("is_dark_theme", s.isDarkTheme)
    }

    /**
     * Cancels the coroutine scope used for async saves.
     *
     * **Note:** Any in-flight [updateSettings] save that has not yet completed will be
     * silently abandoned. The in-memory [settings] StateFlow reflects the last written
     * value, but the persisted store may not. Callers should only invoke [clear] when
     * persistence is no longer required (e.g. on UI teardown).
     */
    fun clear() {
        scope.cancel()
    }
}
