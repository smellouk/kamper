package com.smellouk.kamper.opentelemetry

import io.opentelemetry.api.metrics.Meter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Per-(endpoint, authToken) MeterProvider cache. The first event for a given key
 * builds the SdkMeterProvider and registers gauge callbacks lazily; subsequent
 * events with the same key update the gauge's latest value via the AtomicReference
 * captured by the gauge callback closure.
 */
private data class ProviderKey(val endpoint: String, val authToken: String?)

private class GaugeRegistration(
    val provider: SdkMeterProvider,
    val meter: Meter,
    val latestByGauge: ConcurrentHashMap<String, AtomicReference<Double?>>
)

private val providers = ConcurrentHashMap<ProviderKey, GaugeRegistration>()

@Suppress("NewApi") // OTel Java SDK on Android targets API 24+ (computeIfAbsent, Duration.ofSeconds)
internal actual fun recordGauge(
    gaugeName: String,
    value: Double,
    endpoint: String,
    authToken: String?,
    intervalSeconds: Long
) {
    try {
        val key = ProviderKey(endpoint, authToken)
        val registration = providers.computeIfAbsent(key) { _ ->
            val builder = OtlpHttpMetricExporter.builder().setEndpoint(endpoint)
            if (authToken != null) {
                builder.addHeader("Authorization", authToken)  // caller owns the scheme prefix
            }
            val exporter = builder.build()
            val provider = SdkMeterProvider.builder()
                .registerMetricReader(
                    PeriodicMetricReader.builder(exporter)
                        .setInterval(Duration.ofSeconds(intervalSeconds))
                        .build()
                )
                .build()
            val meter = provider.get("com.smellouk.kamper")
            GaugeRegistration(provider, meter, ConcurrentHashMap())
        }

        val latestRef = registration.latestByGauge.computeIfAbsent(gaugeName) { _ ->
            val ref = AtomicReference<Double?>(null)
            // Register the async gauge ONCE — the callback reads the AtomicReference.
            registration.meter.gaugeBuilder(gaugeName)
                .setUnit(if (gaugeName.contains("usage")) "%" else "")
                .setDescription("Kamper $gaugeName")
                .buildWithCallback { measurement ->
                    val v = ref.get()
                    if (v != null) measurement.record(v)
                }
            ref
        }
        latestRef.set(value)
    } catch (t: Throwable) {
        // Silent: caller already wraps. Defense in depth.
    }
}

internal actual fun shutdownGaugeProvider(endpoint: String, authToken: String?) {
    val key = ProviderKey(endpoint, authToken)
    providers.remove(key)?.provider?.shutdown()
}
