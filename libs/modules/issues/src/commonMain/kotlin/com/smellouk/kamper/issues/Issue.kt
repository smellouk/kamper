package com.smellouk.kamper.issues

data class Issue(
    val id: String,
    val type: IssueType,
    val severity: Severity,
    val message: String,
    val timestampMs: Long,
    val durationMs: Long? = null,
    val stackTrace: String? = null,
    val threadName: String? = null,
    val details: Map<String, String> = emptyMap()
) {
    companion object {
        val INVALID = Issue(
            id = "",
            type = IssueType.SLOW_SPAN,
            severity = Severity.INFO,
            message = "",
            timestampMs = -1L
        )
        val UNSUPPORTED = Issue(
            id = "unsupported",
            type = IssueType.SLOW_SPAN,
            severity = Severity.INFO,
            message = "unsupported",
            timestampMs = -2L
        )
    }
}

enum class IssueType {
    ANR,
    SLOW_COLD_START,
    SLOW_WARM_START,
    SLOW_HOT_START,
    DROPPED_FRAME,
    SLOW_SPAN,
    MEMORY_PRESSURE,
    NEAR_OOM,
    CRASH,
    STRICT_VIOLATION
}

enum class Severity { CRITICAL, ERROR, WARNING, INFO }
