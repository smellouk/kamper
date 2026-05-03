package com.smellouk.konitor.opentelemetry

internal actual fun recordGauge(
    gaugeName: String,
    value: Double,
    endpoint: String,
    authToken: String?,
    intervalSeconds: Long
) {
    // No-op on Kotlin/JS. Per RESEARCH Pitfalls 1 + 2.
}

internal actual fun shutdownGaugeProvider(endpoint: String, authToken: String?) {
    // No-op on Kotlin/JS — recordGauge is a no-op on this platform so no provider exists.
}
