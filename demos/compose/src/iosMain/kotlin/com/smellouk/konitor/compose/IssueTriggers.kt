package com.smellouk.konitor.compose

import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueSpans
import com.smellouk.konitor.issues.IssueType
import com.smellouk.konitor.issues.Severity
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.posix.time
import platform.posix.usleep
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
actual fun triggerSlowSpan() {
    CoroutineScope(Dispatchers.Default).launch {
        IssueSpans.measure("compose-demo-op", thresholdMs = 300L) {
            usleep(800_000u)
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun triggerCrash() {
    val issue = Issue(
        id = "${IssueType.CRASH}_${Random.nextLong().toString(16)}",
        type = IssueType.CRASH,
        severity = Severity.CRITICAL,
        message = "Demo crash from K|Compose/iOS",
        timestampMs = time(null) * 1000L,
        stackTrace = "triggerCrash(IssueTriggers.kt)\nButton.onClick(IssuesTab.kt)",
        threadName = "main"
    )
    IosCrashBridge.onCrash?.invoke(issue)
}
