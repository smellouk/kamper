package com.smellouk.kamper.opentelemetry

import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.IntegrationModule
import com.smellouk.kamper.api.KamperEvent
import com.smellouk.kamper.api.UserEventInfo

/**
 * Forwards Kamper CPU / memory / FPS events to an OTLP-compatible endpoint as
 * gauge measurements, and custom duration events as OTel spans
 * (via opentelemetry-java SDK on JVM and Android; no-op elsewhere).
 *
 * Routing by [KamperEvent.moduleName]:
 *   - "cpu"    -> gauge `kamper.cpu.usage`    (when [OtelConfig.forwardCpu])
 *   - "memory" -> gauge `kamper.memory.usage` (when [OtelConfig.forwardMemory])
 *   - "fps"    -> gauge `kamper.fps.value`    (when [OtelConfig.forwardFps])
 *   - "event"  -> span (duration only; instant events are no-ops per D-31)
 *                 (when [OtelConfig.forwardEvents])
 *   - other    -> ignored
 *
 * Numeric extraction is toString-based (same approach as SentryIntegrationModule —
 * keeps the integration free of compile dependencies on metric modules per RESEARCH
 * Open Question 1).
 *
 * Per Phase 16 D-05 + D-09 + D-10 + D-11; Phase 24 D-29 + D-31 + D-32;
 * threats T-16-02, T-16-04, T-24-F-03 mitigated.
 */
public class OtelIntegrationModule internal constructor(
    private val config: OtelConfig
) : IntegrationModule {

    private val urlIsValid: Boolean =
        config.otlpEndpointUrl.startsWith("http://") || config.otlpEndpointUrl.startsWith("https://")

    override fun onEvent(event: KamperEvent) {
        try {
            // T-16-04 — Info.INVALID sentinel must NEVER reach OTLP.
            if (event.info === Info.INVALID) return
            // V5 input validation — reject non-http(s) endpoints.
            if (!urlIsValid) return

            when (event.moduleName) {
                "cpu" -> {
                    if (!config.forwardCpu) return
                    recordGaugeForMetric(event, "kamper.cpu.usage")
                }
                "memory" -> {
                    if (!config.forwardMemory) return
                    recordGaugeForMetric(event, "kamper.memory.usage")
                }
                "fps" -> {
                    if (!config.forwardFps) return
                    recordGaugeForMetric(event, "kamper.fps.value")
                }
                "event" -> {
                    if (!config.forwardEvents) return
                    // T-24-F-03: cast must not crash; non-UserEventInfo is silently dropped.
                    val info = event.info as? UserEventInfo ?: return
                    // D-31: instant events (durationMs == null) are no-ops for OTel —
                    // spans require both start and end; instant events are covered by
                    // Sentry/Firebase breadcrumbs instead.
                    val durationMs = info.durationMs ?: return
                    recordSpan(
                        name = info.name,
                        startEpochNs = event.timestampMs * NS_PER_MS,
                        durationNs = durationMs * NS_PER_MS,
                        endpoint = config.otlpEndpointUrl,
                        authToken = config.otlpAuthToken
                    )
                }
                else -> Unit
            }
        } catch (t: Throwable) {
            // T-16-02 — never propagate SDK errors to Kamper core.
        }
    }

    override fun clean() {
        try {
            shutdownGaugeProvider(config.otlpEndpointUrl, config.otlpAuthToken)
        } catch (_: Throwable) {
            // Never propagate teardown errors to Kamper core (T-16-02).
        }
        try {
            // D-32: shut down the span provider alongside the gauge provider.
            shutdownSpanProvider(config.otlpEndpointUrl, config.otlpAuthToken)
        } catch (_: Throwable) {
            // Never propagate teardown errors to Kamper core (T-16-02).
        }
    }

    /**
     * Extracts the numeric value for gauge-type metrics (cpu, memory, fps) and
     * records it via [recordGauge]. Extracted into a helper so the "event" branch
     * can have a multi-statement body without duplicating the gauge logic.
     */
    private fun recordGaugeForMetric(event: KamperEvent, gaugeName: String) {
        val value: Float = when (event.moduleName) {
            "memory" -> extractMemoryAllocRatioPercent(event.info) ?: return
            else     -> extractNumberFromToString(event.info) ?: return
        }
        recordGauge(
            gaugeName = gaugeName,
            value = value.toDouble(),
            endpoint = config.otlpEndpointUrl,
            authToken = config.otlpAuthToken,
            intervalSeconds = config.exportIntervalSeconds
        )
    }

    /**
     * Pulls the first floating-point number from `info.toString()`. Used for CPU and FPS:
     * `CpuInfo(totalUseRatio=0.85)` returns 0.85; `FpsInfo(fps=60)` returns 60.
     * Returns null if no number is found.
     */
    private fun extractNumberFromToString(info: Info): Float? {
        val s = info.toString()
        val regex = Regex("-?\\d+(\\.\\d+)?")
        val match = regex.find(s) ?: return null
        return match.value.toFloatOrNull()
    }

    /**
     * Extracts memory allocation as a percentage from `info.toString()` by anchoring on
     * named fields `allocatedInMb` and `maxMemoryInMb` in `MemoryInfo.toString()`.
     * Returns `(allocatedInMb / maxMemoryInMb) * 100f`, or null if extraction fails.
     * The gauge `kamper.memory.usage` is exported with unit `%`, so this value is correct.
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

    private companion object {
        /** Milliseconds to nanoseconds conversion factor (D-31). */
        private const val NS_PER_MS: Long = 1_000_000L
    }
}
