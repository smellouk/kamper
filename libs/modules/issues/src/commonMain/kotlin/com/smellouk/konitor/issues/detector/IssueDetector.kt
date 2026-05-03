package com.smellouk.konitor.issues.detector

import com.smellouk.konitor.issues.Issue

internal interface IssueDetector {
    fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit)
    fun stop()
    fun clean() {}
}
