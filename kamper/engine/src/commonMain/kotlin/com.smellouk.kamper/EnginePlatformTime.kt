package com.smellouk.kamper

/**
 * Wall-clock millisecond timestamp consumed by [Engine.validate]. Internal:
 * not part of the public Kamper API. A separate function from
 * `kamper.api.currentApiTimeMs()` because Kotlin `internal` symbols
 * do not cross module boundaries.
 */
internal expect fun engineCurrentTimeMs(): Long
