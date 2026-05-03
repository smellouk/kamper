package com.smellouk.konitor.opentelemetry

internal actual fun recordSpan(
    name: String,
    startEpochNs: Long,
    durationNs: Long,
    endpoint: String,
    authToken: String?
) {
    // No-op: OTel Java SDK requires JVM. No KMP-native OTel Tracing SDK.
}

internal actual fun shutdownSpanProvider(endpoint: String, authToken: String?) {
    // No-op.
}
