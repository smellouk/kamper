package com.smellouk.kamper.opentelemetry

import com.smellouk.kamper.api.KamperDslMarker

/**
 * DSL config for [OtelIntegrationModule]. Per Phase 16 D-04 + D-10 + D-11.
 *
 * @property otlpEndpointUrl Full OTLP HTTP endpoint URL. Required (e.g.,
 *                           "https://otlp-gateway-prod-us-central-0.grafana.net/otlp/v1/metrics").
 *                           MUST start with `http://` or `https://` — otherwise the integration
 *                           silently disables forwarding (per V5 input validation).
 * @property otlpAuthToken Optional Authorization header value. When non-null, the exporter
 *                         sends `Authorization: <value>` on every request. The caller is
 *                         responsible for including the scheme prefix (e.g., "Bearer glc_eyJ...").
 * @property forwardCpu When true, CpuInfo events become OTLP gauge `kamper.cpu.usage`.
 * @property forwardMemory When true, MemoryInfo events become OTLP gauge `kamper.memory.usage`.
 * @property forwardFps When true, FpsInfo events become OTLP gauge `kamper.fps.value`.
 * @property exportIntervalSeconds Minimum interval between OTLP exports in seconds (default 30).
 */
public data class OtelConfig(
    val otlpEndpointUrl: String,
    val otlpAuthToken: String?,
    val forwardCpu: Boolean,
    val forwardMemory: Boolean,
    val forwardFps: Boolean,
    val exportIntervalSeconds: Long
) {
    // Override to prevent the OTLP auth token from appearing in logs, crash reports, or
    // test failure output via the data class auto-generated toString().
    override fun toString(): String =
        "OtelConfig(otlpEndpointUrl=$otlpEndpointUrl, " +
        "otlpAuthToken=${if (otlpAuthToken != null) "<redacted>" else "null"}, " +
        "forwardCpu=$forwardCpu, forwardMemory=$forwardMemory, forwardFps=$forwardFps, " +
        "exportIntervalSeconds=$exportIntervalSeconds)"

    public companion object {
        public val DEFAULT_INTERVAL_SECONDS: Long = 30L
    }

    @KamperDslMarker
    public class Builder internal constructor() {
        public var otlpAuthToken: String? = null
        public var forwardCpu: Boolean = false
        public var forwardMemory: Boolean = false
        public var forwardFps: Boolean = false
        public var exportIntervalSeconds: Long = DEFAULT_INTERVAL_SECONDS

        internal fun build(otlpEndpointUrl: String): OtelConfig =
            OtelConfig(
                otlpEndpointUrl = otlpEndpointUrl,
                otlpAuthToken = otlpAuthToken,
                forwardCpu = forwardCpu,
                forwardMemory = forwardMemory,
                forwardFps = forwardFps,
                exportIntervalSeconds = exportIntervalSeconds
            )
    }
}
