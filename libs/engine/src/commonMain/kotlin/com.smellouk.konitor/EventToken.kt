package com.smellouk.konitor

/**
 * Opaque handle returned by [Engine.startEvent] and consumed by [Engine.endEvent]
 * (Phase 24 D-02). The constructor is `internal` so tokens cannot be forged outside
 * the engine; the class itself is `public` because [Engine.startEvent] is a public
 * function and must return a referencable type.
 *
 * @property name Mirrors the name passed to [Engine.startEvent].
 * @property startNs Nanosecond timestamp captured at startEvent — used by endEvent
 *   to compute `durationNs = engineCurrentTimeNs() - startNs`.
 */
class EventToken internal constructor(
    val name: String,
    val startNs: Long
)
