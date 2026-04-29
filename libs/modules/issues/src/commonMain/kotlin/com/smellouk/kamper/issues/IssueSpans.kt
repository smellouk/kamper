package com.smellouk.kamper.issues

interface ActiveSpan {
    fun end()
}

object IssueSpans {
    internal var detector: SlowSpanDetectorApi? = null

    fun begin(label: String, thresholdMs: Long? = null): ActiveSpan =
        detector?.begin(label, thresholdMs) ?: NoopSpan

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
