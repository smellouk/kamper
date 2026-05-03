package com.smellouk.konitor

import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueInfo
import com.smellouk.konitor.issues.IssueType
import com.smellouk.konitor.issues.IssuesModule
import com.smellouk.konitor.issues.Severity
import kotlin.random.Random
import platform.posix.gettimeofday
import platform.posix.timeval
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr

internal actual fun installAndListenIssues(onIssue: (IssueInfo) -> Unit) {
    Konitor.install(IssuesModule)
    Konitor.addInfoListener<IssueInfo>(onIssue)
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun KonitorBridge.simulateCrash() {
    val tsMs = memScoped {
        val tv = alloc<timeval>()
        gettimeofday(tv.ptr, null)
        tv.tv_sec * 1_000L + tv.tv_usec / 1_000L
    }
    val issue = Issue(
        id = "CRASH_${Random.nextLong().toString(16)}",
        type = IssueType.CRASH,
        severity = Severity.CRITICAL,
        message = "NSException: Demo crash: triggered by user",
        timestampMs = tsMs,
        threadName = "background"
    )
    Konitor.emit(IssueInfo(issue))
}
