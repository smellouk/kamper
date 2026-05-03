package com.smellouk.konitor.issues.detector

import com.smellouk.konitor.issues.ActiveSpan
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueSpans
import com.smellouk.konitor.issues.IssueType
import com.smellouk.konitor.issues.SlowSpanConfig
import com.smellouk.konitor.issues.SlowSpanDetectorApi
import kotlin.random.Random

internal class SlowSpanDetector(private val config: SlowSpanConfig) : IssueDetector, SlowSpanDetectorApi {
    private val activeSpans = mutableMapOf<String, OpenSpan>()
    private var onIssue: ((Issue) -> Unit)? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        this.onIssue = onIssue
        if (!IssueSpans.detectors.contains(this)) IssueSpans.detectors.add(this)
    }

    override fun stop() {
        IssueSpans.detectors.remove(this)
        onIssue = null
        activeSpans.clear()
    }

    override fun clean() {
        stop()
    }

    override fun begin(label: String, thresholdMs: Long?): ActiveSpan {
        if (!config.isEnabled) return object : ActiveSpan { override fun end() = Unit }
        if (activeSpans.size >= config.maxConcurrentSpans) return object : ActiveSpan { override fun end() = Unit }

        val id = "${label}_${Random.nextLong().toString(16)}"
        val startMs = currentPlatformTimeMs()
        val effectiveThreshold = thresholdMs ?: config.defaultThresholdMs
        val stackTrace = if (config.captureStackTrace) captureCurrentStackTrace() else null

        val span = OpenSpan(
            id = id,
            label = label,
            startMs = startMs,
            thresholdMs = effectiveThreshold,
            stackTrace = stackTrace
        )
        activeSpans[id] = span
        return span
    }

    private inner class OpenSpan(
        val id: String,
        val label: String,
        val startMs: Long,
        val thresholdMs: Long,
        val stackTrace: String?
    ) : ActiveSpan {
        override fun end() {
            activeSpans.remove(id) ?: return
            val durationMs = currentPlatformTimeMs() - startMs
            if (durationMs > thresholdMs) {
                onIssue?.invoke(
                    Issue(
                        id = id,
                        type = IssueType.SLOW_SPAN,
                        severity = config.severity,
                        message = "Span '$label' took ${durationMs}ms (threshold: ${thresholdMs}ms)",
                        timestampMs = currentPlatformTimeMs(),
                        durationMs = durationMs,
                        stackTrace = stackTrace,
                        details = mapOf("label" to label, "threshold" to "${thresholdMs}ms")
                    )
                )
            }
        }
    }
}

internal expect fun currentPlatformTimeMs(): Long
internal expect fun captureCurrentStackTrace(): String
