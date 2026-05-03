package com.smellouk.kamper.opentelemetry

/**
 * Phase 24 D-30. Records a duration event as an OpenTelemetry span via OTLP/HTTP.
 *
 * Real implementations: androidMain + jvmMain (use `SdkTracerProvider` +
 * `OtlpHttpSpanExporter`). All other targets are no-ops because there is no
 * KMP-native OTel Tracing SDK.
 *
 * @param name Span name. Per design (RESEARCH §"Specifics"), no `kamper.` prefix —
 *   span names render in user-facing tracing UIs (Jaeger, Tempo).
 * @param startEpochNs Span start time in nanoseconds since the Unix epoch.
 * @param durationNs Total duration in nanoseconds. End time = startEpochNs + durationNs.
 * @param endpoint OTLP/HTTP endpoint URL.
 * @param authToken Optional bearer token; sent as `Authorization` header when present.
 */
internal expect fun recordSpan(
    name: String,
    startEpochNs: Long,
    durationNs: Long,
    endpoint: String,
    authToken: String?
)

/**
 * Phase 24 D-32. Shut down and remove the cached `SdkTracerProvider` for
 * `(endpoint, authToken)`. Called by [OtelIntegrationModule.clean] alongside
 * [shutdownGaugeProvider].
 */
internal expect fun shutdownSpanProvider(endpoint: String, authToken: String?)
