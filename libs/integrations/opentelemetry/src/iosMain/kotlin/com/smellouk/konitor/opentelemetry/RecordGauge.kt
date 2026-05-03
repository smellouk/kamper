package com.smellouk.konitor.opentelemetry

internal actual fun recordGauge(
    gaugeName: String,
    value: Double,
    endpoint: String,
    authToken: String?,
    intervalSeconds: Long
) {
    // No-op on iOS. opentelemetry-kotlin 0.3.0 has no Metrics API and we do not
    // ship the OTel Java SDK on Native. Per RESEARCH Pitfalls 1 + 2.
}

internal actual fun shutdownGaugeProvider(endpoint: String, authToken: String?) {
    // No-op on iOS — recordGauge is a no-op on this platform so no provider exists.
}
