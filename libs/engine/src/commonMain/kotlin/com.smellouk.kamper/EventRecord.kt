package com.smellouk.kamper

/**
 * Internal record for a logged event (Phase 24 D-09).
 *
 * @property timestampNs Engine nanosecond timestamp from [engineCurrentTimeNs].
 *   On Android matches `SystemClock.elapsedRealtimeNanos()` so it aligns with
 *   counter-track timestamps in the exported Perfetto trace.
 * @property name Caller-supplied event name.
 * @property durationNs `null` = instant event (TYPE_INSTANT in Perfetto);
 *   non-null = completed duration slice (TYPE_SLICE_BEGIN/END pair).
 */
data class EventRecord(
    val timestampNs: Long,
    val name: String,
    val durationNs: Long?
)
