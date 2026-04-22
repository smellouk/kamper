package com.smellouk.kamper.compose

import com.smellouk.kamper.issues.IssueSpans
import java.util.concurrent.Executors

actual fun triggerSlowSpan() {
    Executors.newSingleThreadExecutor().submit {
        IssueSpans.measure("compose-demo-op", thresholdMs = 300L) {
            Thread.sleep(800)
        }
    }
}

actual fun triggerCrash() {
    Thread { throw RuntimeException("Demo crash from K|Compose/Desktop") }
        .also { it.isDaemon = true; it.start() }
}
