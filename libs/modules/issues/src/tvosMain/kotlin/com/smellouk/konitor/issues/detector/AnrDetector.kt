package com.smellouk.konitor.issues.detector

import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random

internal class AnrDetector(private val config: AnrConfig) : IssueDetector {
    private var job: Job? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                val deferred = CompletableDeferred<Unit>()
                CoroutineScope(Dispatchers.Main).launch { deferred.complete(Unit) }
                val responded = withTimeoutOrNull(config.thresholdMs) { deferred.await() }
                if (responded == null && isActive) {
                    onIssue(
                        Issue(
                            id = "${IssueType.ANR}_${Random.nextLong().toString(HEX_RADIX)}",
                            type = IssueType.ANR,
                            severity = config.severity,
                            message = "Main run loop blocked for ≥${config.thresholdMs}ms",
                            timestampMs = currentPlatformTimeMs(),
                            durationMs = config.thresholdMs,
                            threadName = "main"
                        )
                    )
                    kotlinx.coroutines.delay(pingIntervalMs)
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    private companion object {
        const val HEX_RADIX = 16
    }
}
