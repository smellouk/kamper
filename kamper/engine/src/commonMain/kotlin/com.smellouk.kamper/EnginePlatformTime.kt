package com.smellouk.kamper

/**
 * Wall-clock millisecond timestamp consumed by [Engine.validate] and the integration
 * fan-out path in [Engine.install]. Internal: not part of the public Kamper API.
 * A separate function from `kamper.api.currentApiTimeMs()` because Kotlin `internal`
 * symbols do not cross module boundaries.
 *
 * `@PublishedApi` is required so the symbol is accessible from the body of the
 * `inline fun install(...)` that constructs [com.smellouk.kamper.api.KamperEvent]
 * inside a lambda captured by the inline expansion.
 */
@PublishedApi
internal expect fun engineCurrentTimeMs(): Long
