package com.smellouk.kamper.sentry

import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.IntegrationModule
import com.smellouk.kamper.api.KamperEvent
import com.smellouk.kamper.api.UserEventInfo
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.SentryLevel
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

/**
 * Forwards Kamper events to Sentry.
 *
 * Routing by [KamperEvent.moduleName]:
 *   - "issue"  -> Sentry.captureException (when [SentryConfig.forwardIssues])
 *   - "cpu"    -> Sentry breadcrumb       (when [SentryConfig.forwardCpuAbove] != null AND value above threshold)
 *   - "memory" -> Sentry breadcrumb       (when [SentryConfig.forwardMemoryAbove] != null AND value above threshold)
 *   - "fps"    -> Sentry breadcrumb       (when [SentryConfig.forwardFps])
 *   - "event"  -> Sentry breadcrumb       (when [SentryConfig.forwardEvents] AND info is [UserEventInfo]) (D-26)
 *   - other    -> ignored
 *
 * Threshold extraction is intentionally toString()-based: this module does NOT compile-time
 * depend on [com.smellouk.kamper.cpu.CpuInfo] / [com.smellouk.kamper.memory.MemoryInfo] /
 * [com.smellouk.kamper.fps.FpsInfo] — that would make kamper-sentry transitively pull in
 * every metric module. The breadcrumb message captures `info.toString()` directly so the
 * full Info shape is visible in Sentry.
 *
 * For CPU, [extractRatioFromToString] reads `totalUseRatio` (0..1) and scales by 100 before
 * comparing against [SentryConfig.forwardCpuAbove] (in percent).
 * For memory, [extractMemoryAllocRatioPercent] anchors on named fields `allocatedInMb` and
 * `maxMemoryInMb` to compute `allocatedInMb / maxMemoryInMb * 100`, compared against
 * [SentryConfig.forwardMemoryAbove] (in percent).
 * For FPS, the value is forwarded directly without extraction (no threshold, per D-11).
 *
 * Per Phase 16 D-05 + D-08 + D-10 + D-11; threats T-16-02, T-16-04 mitigated by try/catch
 * around every Sentry SDK call AND an Info.INVALID guard.
 */
public class SentryIntegrationModule internal constructor(
    private val config: SentryConfig
) : IntegrationModule {

    init {
        try {
            if (!Sentry.isEnabled()) {
                Sentry.init { options ->
                    options.dsn = config.dsn
                }
            }
            // If Sentry is already enabled (host app owns initialization), skip re-init.
            // This prevents overwriting the host's DSN, options, and SDK integrations.
        } catch (t: Throwable) {
            // Bad DSN, network error during init, or platform-specific runtime missing
            // (e.g., Sentry Cocoa pod missing on macOS). Per threat T-16-01 we never log
            // the DSN itself; we log only the throwable class name + message.
            // Kamper has no logger handle here — the SDK's own error path (if any) is
            // the only signal. We accept the silent failure: future host-side telemetry
            // will surface "no events arriving in Sentry dashboard" naturally.
        }
    }

    override fun onEvent(event: KamperEvent) {
        try {
            // T-16-04 — Info.INVALID sentinel must NEVER reach Sentry.
            if (event.info === Info.INVALID) return

            when (event.moduleName) {
                "issue" -> if (config.forwardIssues) handleIssue(event)
                "cpu"   -> handleCpuMetric(event, threshold = config.forwardCpuAbove, category = "kamper.cpu")
                "memory" -> handleMemoryMetric(event, threshold = config.forwardMemoryAbove, category = "kamper.memory")
                "fps"   -> if (config.forwardFps) handleBreadcrumb(event, category = "kamper.fps", level = SentryLevel.INFO)
                "event" -> handleUserEvent(event)
                else    -> Unit // unsupported moduleName — silently drop
            }
        } catch (t: Throwable) {
            // T-16-02 — never propagate SDK errors to Kamper core.
        }
    }

    override fun clean() {
        // sentry-kotlin-multiplatform 0.13.0 has no required teardown step. If a future
        // version exposes Sentry.close(), wire it here (still inside try/catch).
    }

    private fun handleIssue(event: KamperEvent) {
        val message = "Kamper issue from ${event.moduleName} on ${event.platform}: ${event.info}"
        Sentry.captureException(RuntimeException(message))
    }

    private fun handleCpuMetric(event: KamperEvent, threshold: Float?, category: String) {
        if (threshold == null) return
        val ratio = extractRatioFromToString(event.info) ?: return
        // CpuInfo.totalUseRatio is 0..1; threshold is in percent (e.g., 80f for 80%).
        val percent = ratio * 100f
        if (percent > threshold) {
            handleBreadcrumb(event, category = category, level = SentryLevel.WARNING, valueOverride = percent)
        }
    }

    private fun handleMemoryMetric(event: KamperEvent, threshold: Float?, category: String) {
        if (threshold == null) return
        val percent = extractMemoryAllocRatioPercent(event.info) ?: return
        // threshold is in percent (e.g., 85f for 85%).
        if (percent > threshold) {
            handleBreadcrumb(event, category = category, level = SentryLevel.WARNING, valueOverride = percent)
        }
    }

    private fun handleBreadcrumb(
        event: KamperEvent,
        category: String,
        level: SentryLevel,
        valueOverride: Float? = null
    ) {
        val msg = if (valueOverride != null) {
            "${event.info} (extracted=${valueOverride})"
        } else {
            event.info.toString()
        }
        val crumb = Breadcrumb().apply {
            this.category = category
            this.message = msg
            this.level = level
        }
        Sentry.addBreadcrumb(crumb)
    }

    /**
     * Forwards a [UserEventInfo] custom event to Sentry as a breadcrumb (D-26).
     * Instant events (`durationMs == null`): message is the event name only.
     * Duration events (`durationMs != null`): message is `"<name> (<durationMs> ms)"`.
     * Skipped when [SentryConfig.forwardEvents] is false or `event.info` is not a [UserEventInfo].
     */
    private fun handleUserEvent(event: KamperEvent) {
        if (!config.forwardEvents) return
        val info = event.info as? UserEventInfo ?: return
        val msg = if (info.durationMs != null) {
            "${info.name} (${info.durationMs} ms)"
        } else {
            info.name
        }
        val crumb = Breadcrumb().apply {
            this.category = "kamper.event"
            this.message = msg
            this.level = SentryLevel.INFO
        }
        Sentry.addBreadcrumb(crumb)
    }

    /**
     * Pulls the first floating-point number from `info.toString()`. Used for CPU:
     * `CpuInfo(totalUseRatio=0.85)` returns 0.85 (a 0..1 ratio).
     * Returns null if no number is found.
     */
    private fun extractRatioFromToString(info: Info): Float? {
        val s = info.toString()
        val regex = Regex("-?\\d+(\\.\\d+)?")
        val match = regex.find(s) ?: return null
        return match.value.toFloatOrNull()
    }

    /**
     * Extracts memory allocation as a percentage from `info.toString()` by anchoring
     * on the named fields `allocatedInMb` and `maxMemoryInMb` in `MemoryInfo.toString()`.
     * Returns `(allocatedInMb / maxMemoryInMb) * 100f`, or null if extraction fails.
     */
    private fun extractMemoryAllocRatioPercent(info: Info): Float? {
        val s = info.toString()
        val allocated = Regex("allocatedInMb=(-?\\d+(?:\\.\\d+)?)").find(s)
            ?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        val max = Regex("maxMemoryInMb=(-?\\d+(?:\\.\\d+)?)").find(s)
            ?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        if (max <= 0f) return null
        return (allocated / max) * 100f
    }
}
