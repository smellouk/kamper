package com.smellouk.konitor.opentelemetry

/**
 * Platform bridge to the OTLP exporter. Records [value] as the latest sample of an
 * async gauge named [gaugeName] for the given [endpoint] / [authToken]. The first
 * call for a given (endpoint, authToken) pair sets up a `SdkMeterProvider` with a
 * `PeriodicMetricReader` that flushes at [intervalSeconds] cadence; subsequent calls
 * with the same key reuse the same provider and update the gauge's latest value.
 *
 * Implementations:
 *   - androidMain + jvmMain: real OtlpHttpMetricExporter via opentelemetry-java 1.51.0
 *   - iosMain, macosMain, jsMain, wasmJsMain: NO-OP
 *
 * Implementations MUST swallow any exception (network down, malformed URL, etc.) —
 * caller (OtelIntegrationModule.onEvent) already wraps in try/catch but defense in
 * depth is cheap.
 */
internal expect fun recordGauge(
    gaugeName: String,
    value: Double,
    endpoint: String,
    authToken: String?,
    intervalSeconds: Long
)

/**
 * Shuts down the [SdkMeterProvider] associated with the given (endpoint, authToken) pair
 * and removes it from the provider cache. On JVM and Android this stops the background
 * ScheduledExecutorService started by [PeriodicMetricReader]. On other platforms this is
 * a no-op (the platform actuals are no-ops for [recordGauge] as well).
 */
internal expect fun shutdownGaugeProvider(endpoint: String, authToken: String?)
