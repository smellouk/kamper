package com.smellouk.kamper.ui

data class KamperUiSettings(
    // Visibility in chip (show/hide per metric)
    val showCpu: Boolean = true,
    val showFps: Boolean = true,
    val showMemory: Boolean = true,
    val showNetwork: Boolean = true,
    val showIssues: Boolean = true,
    val showJank: Boolean = false,
    val showGc: Boolean = false,
    val showThermal: Boolean = false,

    // Module enabled (actual start/stop of module)
    val cpuEnabled: Boolean = true,
    val fpsEnabled: Boolean = true,
    val memoryEnabled: Boolean = true,
    val networkEnabled: Boolean = true,
    val issuesEnabled: Boolean = true,
    val jankEnabled: Boolean = false,
    val gcEnabled: Boolean = false,
    val thermalEnabled: Boolean = false,

    // Polling intervals
    val cpuIntervalMs: Long = 1_000L,
    val memoryIntervalMs: Long = 1_000L,
    val networkIntervalMs: Long = 1_000L,
    val issuesIntervalMs: Long = 1_000L,

    // Issues: Slow Span detector
    val slowSpanEnabled: Boolean = true,
    val slowSpanThresholdMs: Long = 1_000L,

    // Issues: Dropped Frames detector
    val droppedFramesEnabled: Boolean = true,
    val droppedFrameThresholdMs: Long = 32L,
    val droppedFrameConsecutiveThreshold: Int = 3,

    // Issues: Crash detector
    val crashEnabled: Boolean = true,

    // Issues: Memory Pressure detector
    val memoryPressureEnabled: Boolean = true,
    val memPressureWarningPct: Float = 0.80f,
    val memPressureCriticalPct: Float = 0.95f,

    // Issues: ANR detector (Android: real ANR; iOS: watchdog)
    val anrEnabled: Boolean = true,
    val anrThresholdMs: Long = 5_000L,

    // Issues: Slow Start detector
    val slowStartEnabled: Boolean = true,
    val slowStartColdThresholdMs: Long = 2_000L,
    val slowStartWarmThresholdMs: Long = 800L
)
