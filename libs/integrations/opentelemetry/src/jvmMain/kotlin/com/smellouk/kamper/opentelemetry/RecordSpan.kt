package com.smellouk.kamper.opentelemetry

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Per-(endpoint, authToken) TracerProvider cache. The first call for a given key
 * builds the SdkTracerProvider and registers a SimpleSpanProcessor; subsequent
 * calls with the same key reuse the cached provider and tracer.
 */
private data class SpanProviderKey(val endpoint: String, val authToken: String?)

private class SpanRegistration(val provider: SdkTracerProvider, val tracer: Tracer)

private val spanProviders: ConcurrentHashMap<SpanProviderKey, SpanRegistration> = ConcurrentHashMap()

internal actual fun recordSpan(
    name: String,
    startEpochNs: Long,
    durationNs: Long,
    endpoint: String,
    authToken: String?
) {
    try {
        val key = SpanProviderKey(endpoint, authToken)
        val reg = spanProviders.computeIfAbsent(key) { _ ->
            val builder = OtlpHttpSpanExporter.builder().setEndpoint(endpoint)
            if (authToken != null) {
                builder.addHeader("Authorization", authToken)
            }
            val exporter = builder.build()
            val provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build()
            SpanRegistration(provider, provider.get("com.smellouk.kamper"))
        }
        val span = reg.tracer.spanBuilder(name)
            .setStartTimestamp(startEpochNs, TimeUnit.NANOSECONDS)
            .startSpan()
        span.end(startEpochNs + durationNs, TimeUnit.NANOSECONDS)
    } catch (_: Throwable) {
        // Swallow per integration contract — host app must not crash on tracing failure.
    }
}

internal actual fun shutdownSpanProvider(endpoint: String, authToken: String?) {
    try {
        val key = SpanProviderKey(endpoint, authToken)
        spanProviders.remove(key)?.provider?.shutdown()
    } catch (_: Throwable) {
        // Swallow.
    }
}
