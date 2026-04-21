package com.smellouk.kamper.issues.detector

import com.smellouk.kamper.issues.Issue

internal interface IssueDetector {
    fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit)
    fun stop()
    fun clean() {}
}
