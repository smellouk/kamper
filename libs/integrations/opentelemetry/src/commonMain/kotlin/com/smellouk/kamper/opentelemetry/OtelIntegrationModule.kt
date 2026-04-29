package com.smellouk.kamper.opentelemetry

import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.IntegrationModule
import com.smellouk.kamper.api.KamperEvent

/**
 * Forwards Kamper CPU / memory / FPS events to an OTLP-compatible endpoint as
 * gauge measurements (via opentelemetry-java SDK on JVM and Android; no-op elsewhere).
 *
 * Routing by [KamperEvent.moduleName]:
 *   - "cpu"    -> gauge `kamper.cpu.usage`    (when [OtelConfig.forwardCpu])
 *   - "memory" -> gauge `kamper.memory.usage` (when [OtelConfig.forwardMemory])
 *   - "fps"    -> gauge `kamper.fps.value`    (when [OtelConfig.forwardFps])
 *   - other    -> ignored
 *
 * Numeric extraction is toString-based (same approach as SentryIntegrationModule —
 * keeps the integration free of compile dependencies on metric modules per RESEARCH
 * Open Question 1).
 *
 * Per Phase 16 D-05 + D-09 + D-10 + D-11; threats T-16-02, T-16-04 mitigated.
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

            val name: String = when (event.moduleName) {
                "cpu"    -> if (config.forwardCpu) "kamper.cpu.usage" else return
                "memory" -> if (config.forwardMemory) "kamper.memory.usage" else return
                "fps"    -> if (config.forwardFps) "kamper.fps.value" else return
                else     -> return
            }

            val value: Float = when (event.moduleName) {
                "memory" -> extractMemoryAllocRatioPercent(event.info) ?: return
                else     -> extractNumberFromToString(event.info) ?: return
            }

            recordGauge(
                gaugeName = name,
                value = value.toDouble(),
                endpoint = config.otlpEndpointUrl,
                authToken = config.otlpAuthToken,
                intervalSeconds = config.exportIntervalSeconds
            )
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
}
