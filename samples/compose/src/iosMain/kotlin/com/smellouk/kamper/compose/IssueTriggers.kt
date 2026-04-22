package com.smellouk.kamper.compose

import com.smellouk.kamper.issues.IssueSpans
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import platform.posix.usleep

@OptIn(ExperimentalForeignApi::class)
actual fun triggerSlowSpan() {
    CoroutineScope(Dispatchers.Default).launch {
        IssueSpans.measure("compose-demo-op", thresholdMs = 300L) {
            usleep(800_000u)
        }
    }
}

actual fun triggerCrash() {
    CoroutineScope(Dispatchers.Default).launch {
        throw RuntimeException("Demo crash from K|Compose/iOS")
    }
}
