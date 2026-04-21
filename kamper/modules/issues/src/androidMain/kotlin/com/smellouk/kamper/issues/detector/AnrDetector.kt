package com.smellouk.kamper.issues.detector

import android.os.Handler
import android.os.Looper
import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueType
import kotlin.random.Random

internal class AnrDetector(private val config: AnrConfig) : IssueDetector {
    @Volatile private var tickReceived = true
    private var watchdogThread: Thread? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        val handler = Handler(Looper.getMainLooper())
        watchdogThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    tickReceived = false
                    handler.post { tickReceived = true }
                    Thread.sleep(config.thresholdMs)
                    if (!tickReceived) {
                        if (config.ignoreWhenDebuggerAttached && isDebuggerAttached()) {
                            continue
                        }
                        val mainThread = Looper.getMainLooper().thread
                        val stackTrace = if (config.captureThreadDump) {
                            mainThread.stackTrace.joinToString("\n") { "\tat $it" }
                        } else null

                        onIssue(
                            Issue(
                                id = "${IssueType.ANR}_${Random.nextLong().toString(16)}",
                                type = IssueType.ANR,
                                severity = config.severity,
                                message = "Main thread blocked for ≥${config.thresholdMs}ms",
                                timestampMs = currentPlatformTimeMs(),
                                durationMs = config.thresholdMs,
                                stackTrace = stackTrace,
                                threadName = mainThread.name
                            )
                        )
                        Thread.sleep(pingIntervalMs)
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }.also {
            it.name = "Kamper-AnrWatchdog"
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        watchdogThread?.interrupt()
        watchdogThread = null
    }

    private fun isDebuggerAttached(): Boolean =
        android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()
}
