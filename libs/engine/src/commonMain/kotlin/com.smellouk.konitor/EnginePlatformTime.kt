package com.smellouk.konitor

/**
 * Wall-clock millisecond timestamp consumed by [Engine.validate] and the integration
 * fan-out path in [Engine.install]. Internal: not part of the public Konitor API.
 * A separate function from `konitor.api.currentApiTimeMs()` because Kotlin `internal`
 * symbols do not cross module boundaries.
 *
 * `@PublishedApi` is required so the symbol is accessible from the body of the
 * `inline fun install(...)` that constructs [com.smellouk.konitor.api.KonitorEvent]
 * inside a lambda captured by the inline expansion.
 */
@PublishedApi
internal expect fun engineCurrentTimeMs(): Long

/**
 * Nanosecond monotonic timestamp used by `Engine.logEvent`/`startEvent` (Phase 24).
 *
 * Alignment contract:
 *   On androidMain MUST be the same clock as `RecordingManager.nowNs()` so `EventRecord.timestampNs`
 *   matches counter-track timestamps in the exported Perfetto trace (`CLOCK_BOOTTIME` = 6).
 *
 *   Other platforms: any monotonic source. Apple Perfetto export is currently a stub
 *   (`ByteArray(0)`); JVM has no Perfetto export. So clock-source equivalence between
 *   nanos and millis on those platforms is acceptable.
 */
@PublishedApi
internal expect fun engineCurrentTimeNs(): Long
