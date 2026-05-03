package com.smellouk.konitor.opentelemetry

import com.smellouk.konitor.api.KonitorDslMarker

/**
 * Public DSL factory for the OpenTelemetry OTLP integration module.
 *
 * Usage:
 * ```
 * Konitor
 *   .install(CpuModule)
 *   .install(MemoryModule)
 *   .addIntegration(
 *     OpenTelemetryModule(otlpEndpointUrl = "https://otlp-gateway.example.com/v1/metrics") {
 *       otlpAuthToken = "Bearer glc_eyJ..."  // full header value, caller owns the scheme prefix
 *       forwardCpu = true
 *       forwardMemory = true
 *       forwardFps = false
 *       exportIntervalSeconds = 30L
 *     }
 *   )
 * ```
 *
 * Per Phase 16 D-04 + D-09. The endpoint URL is required and must start with `http://`
 * or `https://` — invalid URLs cause the module to silently disable forwarding.
 *
 * Platform behavior:
 *   - Android, JVM -> real OTLP gauge export via opentelemetry-java 1.51.0
 *   - iOS, macOS, JS, WasmJS -> no-op (per RESEARCH Pitfalls 1 + 2)
 */
@Suppress("FunctionNaming")
public fun OpenTelemetryModule(
    otlpEndpointUrl: String,
    builder: OtelConfig.Builder.() -> Unit = {}
): OtelIntegrationModule =
    OtelIntegrationModule(OtelConfig.Builder().apply(builder).build(otlpEndpointUrl))
