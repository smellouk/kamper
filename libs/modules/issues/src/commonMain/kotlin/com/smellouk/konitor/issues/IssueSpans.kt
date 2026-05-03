package com.smellouk.konitor.issues

interface ActiveSpan {
    fun end()
}

object IssueSpans {
    internal val detectors = mutableListOf<SlowSpanDetectorApi>()

    fun begin(label: String, thresholdMs: Long? = null): ActiveSpan {
        if (detectors.isEmpty()) return NoopSpan
        val spans = detectors.map { it.begin(label, thresholdMs) }
        return if (spans.size == 1) spans[0]
        else object : ActiveSpan { override fun end() = spans.forEach { it.end() } }
    }

    fun <T> measure(label: String, thresholdMs: Long? = null, block: () -> T): T {
        val span = begin(label, thresholdMs)
        return try {
            block()
        } finally {
            span.end()
        }
    }
}

internal interface SlowSpanDetectorApi {
    fun begin(label: String, thresholdMs: Long?): ActiveSpan
}

private object NoopSpan : ActiveSpan {
    override fun end() = Unit
}
