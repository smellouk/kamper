package com.smellouk.kamper.issues

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

data class IssuesConfig(
    override val isEnabled: Boolean = true,
    override val intervalInMs: Long = 1_000L,
    val logger: Logger = Logger.EMPTY,
    val maxStoredIssues: Int = 200,
    val slowSpan: SlowSpanConfig = SlowSpanConfig(),
    val droppedFrames: DroppedFramesConfig = DroppedFramesConfig(),
    val crash: CrashConfig = CrashConfig(),
    val memoryPressure: MemoryPressureConfig = MemoryPressureConfig()
) : Config {
    companion object {
        val DEFAULT = IssuesConfig()
    }

    object Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger
        var maxStoredIssues: Int = DEFAULT.maxStoredIssues
        var slowSpan: SlowSpanConfig = DEFAULT.slowSpan
        var droppedFrames: DroppedFramesConfig = DEFAULT.droppedFrames
        var crash: CrashConfig = DEFAULT.crash
        var memoryPressure: MemoryPressureConfig = DEFAULT.memoryPressure

        fun slowSpan(block: SlowSpanConfig.Builder.() -> Unit) {
            slowSpan = SlowSpanConfig.Builder.apply(block).build()
        }

        fun droppedFrames(block: DroppedFramesConfig.Builder.() -> Unit) {
            droppedFrames = DroppedFramesConfig.Builder.apply(block).build()
        }

        fun crash(block: CrashConfig.Builder.() -> Unit) {
            crash = CrashConfig.Builder.apply(block).build()
        }

        fun memoryPressure(block: MemoryPressureConfig.Builder.() -> Unit) {
            memoryPressure = MemoryPressureConfig.Builder.apply(block).build()
        }

        fun build(): IssuesConfig = IssuesConfig(
            isEnabled, intervalInMs, logger, maxStoredIssues,
            slowSpan, droppedFrames, crash, memoryPressure
        )
    }
}

// ── Sub-configs (common to all platforms) ─────────────────────────────────────

data class SlowSpanConfig(
    val isEnabled: Boolean = true,
    val defaultThresholdMs: Long = 1_000L,
    val captureStackTrace: Boolean = false,
    val autoCloseAfterMs: Long = 30_000L,
    val maxConcurrentSpans: Int = 50,
    val severity: Severity = Severity.WARNING
) {
    object Builder {
        var isEnabled: Boolean = SlowSpanConfig().isEnabled
        var defaultThresholdMs: Long = SlowSpanConfig().defaultThresholdMs
        var captureStackTrace: Boolean = SlowSpanConfig().captureStackTrace
        var autoCloseAfterMs: Long = SlowSpanConfig().autoCloseAfterMs
        var maxConcurrentSpans: Int = SlowSpanConfig().maxConcurrentSpans
        var severity: Severity = SlowSpanConfig().severity
        fun build() = SlowSpanConfig(isEnabled, defaultThresholdMs, captureStackTrace, autoCloseAfterMs, maxConcurrentSpans, severity)
    }
}

data class DroppedFramesConfig(
    val isEnabled: Boolean = true,
    val frameThresholdMs: Long = 32L,
    val consecutiveFramesThreshold: Int = 3,
    val cooldownMs: Long = 2_000L,
    val severity: Severity = Severity.WARNING
) {
    object Builder {
        var isEnabled: Boolean = DroppedFramesConfig().isEnabled
        var frameThresholdMs: Long = DroppedFramesConfig().frameThresholdMs
        var consecutiveFramesThreshold: Int = DroppedFramesConfig().consecutiveFramesThreshold
        var cooldownMs: Long = DroppedFramesConfig().cooldownMs
        var severity: Severity = DroppedFramesConfig().severity
        fun build() = DroppedFramesConfig(isEnabled, frameThresholdMs, consecutiveFramesThreshold, cooldownMs, severity)
    }
}

data class CrashConfig(
    val isEnabled: Boolean = true,
    val captureAllThreads: Boolean = false,
    val includeDeviceContext: Boolean = true,
    val chainToPreviousHandler: Boolean = true,
    val persistToDisk: Boolean = true,
    val severity: Severity = Severity.CRITICAL
) {
    object Builder {
        var isEnabled: Boolean = CrashConfig().isEnabled
        var captureAllThreads: Boolean = CrashConfig().captureAllThreads
        var includeDeviceContext: Boolean = CrashConfig().includeDeviceContext
        var chainToPreviousHandler: Boolean = CrashConfig().chainToPreviousHandler
        var persistToDisk: Boolean = CrashConfig().persistToDisk
        var severity: Severity = CrashConfig().severity
        fun build() = CrashConfig(isEnabled, captureAllThreads, includeDeviceContext, chainToPreviousHandler, persistToDisk, severity)
    }
}

data class MemoryPressureConfig(
    val isEnabled: Boolean = true,
    val warningThresholdPercent: Float = 0.80f,
    val criticalThresholdPercent: Float = 0.95f,
    val cooldownMs: Long = 10_000L,
    val checkIntervalMs: Long = 2_000L
) {
    object Builder {
        var isEnabled: Boolean = MemoryPressureConfig().isEnabled
        var warningThresholdPercent: Float = MemoryPressureConfig().warningThresholdPercent
        var criticalThresholdPercent: Float = MemoryPressureConfig().criticalThresholdPercent
        var cooldownMs: Long = MemoryPressureConfig().cooldownMs
        var checkIntervalMs: Long = MemoryPressureConfig().checkIntervalMs
        fun build() = MemoryPressureConfig(isEnabled, warningThresholdPercent, criticalThresholdPercent, cooldownMs, checkIntervalMs)
    }
}

// ── Platform-specific sub-configs (shared data classes, used only where applicable) ──

data class AnrConfig(
    val isEnabled: Boolean = true,
    val thresholdMs: Long = 5_000L,
    val captureThreadDump: Boolean = true,
    val ignoreWhenDebuggerAttached: Boolean = true,
    val severity: Severity = Severity.CRITICAL
) {
    object Builder {
        var isEnabled: Boolean = AnrConfig().isEnabled
        var thresholdMs: Long = AnrConfig().thresholdMs
        var captureThreadDump: Boolean = AnrConfig().captureThreadDump
        var ignoreWhenDebuggerAttached: Boolean = AnrConfig().ignoreWhenDebuggerAttached
        var severity: Severity = AnrConfig().severity
        fun build() = AnrConfig(isEnabled, thresholdMs, captureThreadDump, ignoreWhenDebuggerAttached, severity)
    }
}

data class SlowStartConfig(
    val isEnabled: Boolean = true,
    val coldStartThresholdMs: Long = 2_000L,
    val warmStartThresholdMs: Long = 800L,
    val hotStartThresholdMs: Long = 200L,
    val severity: Severity = Severity.ERROR
) {
    object Builder {
        var isEnabled: Boolean = SlowStartConfig().isEnabled
        var coldStartThresholdMs: Long = SlowStartConfig().coldStartThresholdMs
        var warmStartThresholdMs: Long = SlowStartConfig().warmStartThresholdMs
        var hotStartThresholdMs: Long = SlowStartConfig().hotStartThresholdMs
        var severity: Severity = SlowStartConfig().severity
        fun build() = SlowStartConfig(isEnabled, coldStartThresholdMs, warmStartThresholdMs, hotStartThresholdMs, severity)
    }
}

data class StrictModeConfig(
    val isEnabled: Boolean = true,
    val detectDiskReads: Boolean = true,
    val detectDiskWrites: Boolean = true,
    val detectNetwork: Boolean = true,
    val detectCustomSlowCalls: Boolean = true,
    val detectUnbufferedIo: Boolean = false,
    val detectActivityLeaks: Boolean = true,
    val detectLeakedClosableObjects: Boolean = true,
    val detectLeakedSqliteObjects: Boolean = true,
    val severity: Severity = Severity.ERROR
) {
    object Builder {
        var isEnabled: Boolean = StrictModeConfig().isEnabled
        var detectDiskReads: Boolean = StrictModeConfig().detectDiskReads
        var detectDiskWrites: Boolean = StrictModeConfig().detectDiskWrites
        var detectNetwork: Boolean = StrictModeConfig().detectNetwork
        var detectCustomSlowCalls: Boolean = StrictModeConfig().detectCustomSlowCalls
        var detectUnbufferedIo: Boolean = StrictModeConfig().detectUnbufferedIo
        var detectActivityLeaks: Boolean = StrictModeConfig().detectActivityLeaks
        var detectLeakedClosableObjects: Boolean = StrictModeConfig().detectLeakedClosableObjects
        var detectLeakedSqliteObjects: Boolean = StrictModeConfig().detectLeakedSqliteObjects
        var severity: Severity = StrictModeConfig().severity
        fun build() = StrictModeConfig(
            isEnabled, detectDiskReads, detectDiskWrites, detectNetwork,
            detectCustomSlowCalls, detectUnbufferedIo, detectActivityLeaks,
            detectLeakedClosableObjects, detectLeakedSqliteObjects, severity
        )
    }
}
