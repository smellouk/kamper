package com.smellouk.konitor.issues.detector

import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import java.awt.EventQueue
import kotlin.random.Random

internal class AnrDetector(private val config: AnrConfig) : IssueDetector {
    @Volatile private var tickReceived = true
    private var watchdogThread: Thread? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        watchdogThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    tickReceived = false
                    EventQueue.invokeLater { tickReceived = true }
                    Thread.sleep(config.thresholdMs)
                    if (!tickReceived) {
                        val edtFrames = Thread.getAllStackTraces().entries
                            .firstOrNull { it.key.name == "AWT-EventQueue-0" }
                            ?.value
                        val stackTrace = if (config.captureThreadDump && edtFrames != null) {
                            edtFrames.joinToString("\n") { "\tat $it" }
                        } else {
                            null
                        }

                        onIssue(
                            Issue(
                                id = "${IssueType.ANR}_${Random.nextLong().toString(HEX_RADIX)}",
                                type = IssueType.ANR,
                                severity = config.severity,
                                message = "EDT blocked for ≥${config.thresholdMs}ms",
                                timestampMs = currentPlatformTimeMs(),
                                durationMs = config.thresholdMs,
                                stackTrace = stackTrace,
                                threadName = "AWT-EventQueue-0"
                            )
                        )
                        Thread.sleep(pingIntervalMs)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }.also {
            it.name = "Konitor-AnrWatchdog"
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        watchdogThread?.interrupt()
        watchdogThread?.join(config.thresholdMs + JOIN_GRACE_MS)
        watchdogThread = null
    }

    private companion object {
        const val HEX_RADIX = 16
        const val JOIN_GRACE_MS = 500L
    }
}
