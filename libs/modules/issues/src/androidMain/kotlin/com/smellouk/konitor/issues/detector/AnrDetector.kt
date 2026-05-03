package com.smellouk.konitor.issues.detector

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.smellouk.konitor.issues.AnrConfig
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import kotlin.random.Random

internal class AnrDetector(private val config: AnrConfig) : IssueDetector {
    @Volatile private var tickReceived = true
    @Volatile private var stopped = false
    @Volatile private var watchdogThread: Thread? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        if (watchdogThread?.isAlive == true) return
        stopped = false
        val handler = Handler(Looper.getMainLooper())
        watchdogThread = Thread {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    tickReceived = false
                    handler.post { tickReceived = true }
                    Thread.sleep(config.thresholdMs)
                    if (!tickReceived) {
                        val debuggerAttached = config.ignoreWhenDebuggerAttached && isDebuggerAttached()
                        if (!debuggerAttached) {
                            if (stopped) {
                                break
                            }
                            val mainThread = Looper.getMainLooper().thread
                            val stackTrace = if (config.captureThreadDump) {
                                mainThread.stackTrace.joinToString("\n") { "\tat $it" }
                            } else {
                                null
                            }

                            onIssue(
                                Issue(
                                    id = "${IssueType.ANR}_${Random.nextLong().toString(HEX_RADIX)}",
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
        stopped = true
        watchdogThread?.interrupt()
        watchdogThread?.join(config.thresholdMs + JOIN_GRACE_MS)
        if (watchdogThread?.isAlive == true) {
            Log.w(TAG, "AnrDetector: watchdog thread did not exit in time")
        }
        watchdogThread = null
    }

    private fun isDebuggerAttached(): Boolean =
        android.os.Debug.isDebuggerConnected() || android.os.Debug.waitingForDebugger()

    private companion object {
        const val TAG = "AnrDetector"
        const val JOIN_GRACE_MS = 500L
        const val HEX_RADIX = 16
    }
}
