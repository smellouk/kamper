package com.smellouk.kamper.issues

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

/**
 * Configuration for the Issues module — the umbrella module that monitors slow spans, dropped
 * frames, crashes, memory pressure, ANRs, slow starts, and StrictMode violations.
 *
 * @property logger Logger used for module-internal diagnostic output. Defaults to [Logger.EMPTY].
 * @property onDroppedIssue Callback invoked when an issue is evicted from the internal accumulator
 *   because [maxStoredIssues] was reached. See [DroppedIssueEvent] for the payload.
 * @property maxStoredIssues Maximum number of issues retained in the internal accumulator. When the
 *   cap is reached, the oldest issue is dropped (FIFO) and [onDroppedIssue] is invoked. Default: 200.
 * @property slowSpan Configuration for the slow-span detector. See [SlowSpanConfig].
 * @property droppedFrames Configuration for the dropped-frames detector. See [DroppedFramesConfig].
 * @property crash Configuration for the crash detector. See [CrashConfig].
 * @property memoryPressure Configuration for the memory-pressure detector. See [MemoryPressureConfig].
 */
data class IssuesConfig(
    override val isEnabled: Boolean = true,
    override val intervalInMs: Long = 1_000L,
    val logger: Logger = Logger.EMPTY,
    val onDroppedIssue: ((DroppedIssueEvent) -> Unit)? = null,
    val maxStoredIssues: Int = 200,
    val slowSpan: SlowSpanConfig = SlowSpanConfig(),
    val droppedFrames: DroppedFramesConfig = DroppedFramesConfig(),
    val crash: CrashConfig = CrashConfig(),
    val memoryPressure: MemoryPressureConfig = MemoryPressureConfig()
) : Config {
    companion object {
        val DEFAULT = IssuesConfig()
    }

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger
        var onDroppedIssue: ((DroppedIssueEvent) -> Unit)? = DEFAULT.onDroppedIssue
        var maxStoredIssues: Int = DEFAULT.maxStoredIssues
        var slowSpan: SlowSpanConfig = DEFAULT.slowSpan
        var droppedFrames: DroppedFramesConfig = DEFAULT.droppedFrames
        var crash: CrashConfig = DEFAULT.crash
        var memoryPressure: MemoryPressureConfig = DEFAULT.memoryPressure

        fun slowSpan(block: SlowSpanConfig.Builder.() -> Unit) {
            slowSpan = SlowSpanConfig.Builder().apply(block).build()
        }

        fun droppedFrames(block: DroppedFramesConfig.Builder.() -> Unit) {
            droppedFrames = DroppedFramesConfig.Builder().apply(block).build()
        }

        fun crash(block: CrashConfig.Builder.() -> Unit) {
            crash = CrashConfig.Builder().apply(block).build()
        }

        fun memoryPressure(block: MemoryPressureConfig.Builder.() -> Unit) {
            memoryPressure = MemoryPressureConfig.Builder().apply(block).build()
        }

        fun build(): IssuesConfig = IssuesConfig(
            isEnabled, intervalInMs, logger, onDroppedIssue, maxStoredIssues,
            slowSpan, droppedFrames, crash, memoryPressure
        )
    }
}

// ── Sub-configs (common to all platforms) ─────────────────────────────────────

/**
 * Configuration for the slow-span detector. A "slow span" is a code region wrapped in a Kamper
 * tracing API call that exceeds the configured threshold while open.
 *
 * @property defaultThresholdMs Default span-duration threshold above which a span is reported as slow.
 * @property captureStackTrace If true, capture a stack trace when a slow span fires. Off by default
 *   to avoid the cost of stack walking on the hot path.
 * @property autoCloseAfterMs Maximum lifetime of a span before it is auto-closed by the detector,
 *   protecting against leaks from forgotten close() calls.
 * @property maxConcurrentSpans Maximum number of concurrently-open spans tracked by the detector.
 * @property severity Severity assigned to a slow-span issue. See [Severity].
 */
data class SlowSpanConfig(
    val isEnabled: Boolean = true,
    val defaultThresholdMs: Long = 1_000L,
    val captureStackTrace: Boolean = false,
    val autoCloseAfterMs: Long = 30_000L,
    val maxConcurrentSpans: Int = 50,
    val severity: Severity = Severity.WARNING
) {
    class Builder {
        var isEnabled: Boolean = SlowSpanConfig().isEnabled
        var defaultThresholdMs: Long = SlowSpanConfig().defaultThresholdMs
        var captureStackTrace: Boolean = SlowSpanConfig().captureStackTrace
        var autoCloseAfterMs: Long = SlowSpanConfig().autoCloseAfterMs
        var maxConcurrentSpans: Int = SlowSpanConfig().maxConcurrentSpans
        var severity: Severity = SlowSpanConfig().severity
        fun build() = SlowSpanConfig(isEnabled, defaultThresholdMs, captureStackTrace, autoCloseAfterMs, maxConcurrentSpans, severity)
    }
}

/**
 * Configuration for the dropped-frames detector. A frame is "dropped" when its render time exceeds
 * [frameThresholdMs] (default 32 ms ≈ two refresh intervals on a 60 Hz display).
 *
 * @property frameThresholdMs Frame render time above which the frame is counted as dropped.
 * @property consecutiveFramesThreshold Number of consecutive dropped frames required to fire an issue.
 * @property cooldownMs Minimum time between consecutive dropped-frame issues to avoid log flooding.
 * @property severity Severity assigned to dropped-frame issues. See [Severity].
 */
data class DroppedFramesConfig(
    val isEnabled: Boolean = true,
    val frameThresholdMs: Long = 32L,
    val consecutiveFramesThreshold: Int = 3,
    val cooldownMs: Long = 2_000L,
    val severity: Severity = Severity.WARNING
) {
    class Builder {
        var isEnabled: Boolean = DroppedFramesConfig().isEnabled
        var frameThresholdMs: Long = DroppedFramesConfig().frameThresholdMs
        var consecutiveFramesThreshold: Int = DroppedFramesConfig().consecutiveFramesThreshold
        var cooldownMs: Long = DroppedFramesConfig().cooldownMs
        var severity: Severity = DroppedFramesConfig().severity
        fun build() = DroppedFramesConfig(isEnabled, frameThresholdMs, consecutiveFramesThreshold, cooldownMs, severity)
    }
}

/**
 * Configuration for the crash detector. When enabled, installs a default uncaught-exception handler
 * that captures crashes and emits an [Issue] of type CRASH.
 *
 * If your app already integrates Crashlytics, Sentry, or another crash reporter, Kamper logs a
 * warning to logcat at install time naming the displaced handler. See `SECURITY.md` for guidance
 * on coexisting with third-party crash reporters.
 *
 * @property captureAllThreads If true, dump all thread stack traces on crash, not just the
 *   crashing thread.
 * @property includeDeviceContext Include device metadata (model, OS version, locale) in the issue.
 * @property chainToPreviousHandler If true, after Kamper handles the crash, delegate to the
 *   previously-installed uncaught-exception handler.
 * @property persistToDisk Reserved for a future release. Currently has no effect — crash reports
 *   are not written to disk.
 * @property severity Severity assigned to crash issues. See [Severity].
 */
data class CrashConfig(
    val isEnabled: Boolean = true,
    val captureAllThreads: Boolean = false,
    val includeDeviceContext: Boolean = true,
    val chainToPreviousHandler: Boolean = true,
    val persistToDisk: Boolean = true,
    val severity: Severity = Severity.CRITICAL
) {
    class Builder {
        var isEnabled: Boolean = CrashConfig().isEnabled
        var captureAllThreads: Boolean = CrashConfig().captureAllThreads
        var includeDeviceContext: Boolean = CrashConfig().includeDeviceContext
        var chainToPreviousHandler: Boolean = CrashConfig().chainToPreviousHandler
        var persistToDisk: Boolean = CrashConfig().persistToDisk
        var severity: Severity = CrashConfig().severity
        fun build() = CrashConfig(isEnabled, captureAllThreads, includeDeviceContext, chainToPreviousHandler, persistToDisk, severity)
    }
}

/**
 * Configuration for the memory-pressure detector. Polls heap utilisation against `Runtime.maxMemory()`
 * and emits an issue when usage crosses the warning or critical threshold.
 *
 * @property warningThresholdPercent Heap usage fraction at which a WARNING-severity issue fires
 *   (default 0.80 = 80 percent of max heap).
 * @property criticalThresholdPercent Heap usage fraction at which a CRITICAL-severity issue fires.
 * @property cooldownMs Minimum time between consecutive memory-pressure issues to avoid flooding.
 * @property checkIntervalMs Polling interval for memory-pressure checks.
 */
data class MemoryPressureConfig(
    val isEnabled: Boolean = true,
    val warningThresholdPercent: Float = 0.80f,
    val criticalThresholdPercent: Float = 0.95f,
    val cooldownMs: Long = 10_000L,
    val checkIntervalMs: Long = 2_000L
) {
    class Builder {
        var isEnabled: Boolean = MemoryPressureConfig().isEnabled
        var warningThresholdPercent: Float = MemoryPressureConfig().warningThresholdPercent
        var criticalThresholdPercent: Float = MemoryPressureConfig().criticalThresholdPercent
        var cooldownMs: Long = MemoryPressureConfig().cooldownMs
        var checkIntervalMs: Long = MemoryPressureConfig().checkIntervalMs
        fun build() = MemoryPressureConfig(isEnabled, warningThresholdPercent, criticalThresholdPercent, cooldownMs, checkIntervalMs)
    }
}

// ── Platform-specific sub-configs (shared data classes, used only where applicable) ──

/**
 * Configuration for the ANR (Application Not Responding) detector. Posts a heartbeat message to the
 * main thread and reports an issue if the heartbeat is not received within [thresholdMs].
 *
 * @property thresholdMs Main-thread blocking duration above which an ANR is reported. Android's
 *   own ANR threshold is 5 seconds — match this default.
 * @property captureThreadDump Include a snapshot of all thread stack traces in the issue payload.
 * @property ignoreWhenDebuggerAttached Suppress ANR detection while a debugger is attached, since
 *   breakpoints look identical to a hung main thread.
 * @property severity Severity assigned to ANR issues. See [Severity].
 */
data class AnrConfig(
    val isEnabled: Boolean = true,
    val thresholdMs: Long = 5_000L,
    val captureThreadDump: Boolean = true,
    val ignoreWhenDebuggerAttached: Boolean = true,
    val severity: Severity = Severity.CRITICAL
) {
    class Builder {
        var isEnabled: Boolean = AnrConfig().isEnabled
        var thresholdMs: Long = AnrConfig().thresholdMs
        var captureThreadDump: Boolean = AnrConfig().captureThreadDump
        var ignoreWhenDebuggerAttached: Boolean = AnrConfig().ignoreWhenDebuggerAttached
        var severity: Severity = AnrConfig().severity
        fun build() = AnrConfig(isEnabled, thresholdMs, captureThreadDump, ignoreWhenDebuggerAttached, severity)
    }
}

/**
 * Configuration for the slow-start detector. Categorizes app start as cold, warm, or hot based on
 * the elapsed time from process creation to first frame, and reports an issue if the corresponding
 * threshold is exceeded.
 *
 * @property coldStartThresholdMs Threshold above which a cold start (first launch) is considered slow.
 * @property warmStartThresholdMs Threshold above which a warm start (foreground re-entry from
 *   background) is considered slow.
 * @property hotStartThresholdMs Threshold above which a hot start (already-running activity
 *   refocused) is considered slow.
 * @property severity Severity assigned to slow-start issues. See [Severity].
 */
data class SlowStartConfig(
    val isEnabled: Boolean = true,
    val coldStartThresholdMs: Long = 2_000L,
    val warmStartThresholdMs: Long = 800L,
    val hotStartThresholdMs: Long = 200L,
    val severity: Severity = Severity.ERROR
) {
    class Builder {
        var isEnabled: Boolean = SlowStartConfig().isEnabled
        var coldStartThresholdMs: Long = SlowStartConfig().coldStartThresholdMs
        var warmStartThresholdMs: Long = SlowStartConfig().warmStartThresholdMs
        var hotStartThresholdMs: Long = SlowStartConfig().hotStartThresholdMs
        var severity: Severity = SlowStartConfig().severity
        fun build() = SlowStartConfig(isEnabled, coldStartThresholdMs, warmStartThresholdMs, hotStartThresholdMs, severity)
    }
}

/**
 * Configuration for the StrictMode detector. Wraps Android's StrictMode policy violations in
 * Kamper [Issue] reports so they appear alongside other performance issues.
 *
 * Each `detect*` flag toggles a specific StrictMode probe; see the Android `StrictMode.ThreadPolicy`
 * and `StrictMode.VmPolicy` documentation for individual semantics.
 *
 * @property severity Severity assigned to StrictMode issues. See [Severity].
 */
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
    class Builder {
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
