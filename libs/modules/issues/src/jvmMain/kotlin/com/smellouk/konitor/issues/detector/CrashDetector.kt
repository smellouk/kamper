package com.smellouk.konitor.issues.detector

import com.smellouk.konitor.issues.CrashConfig
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import kotlin.random.Random

internal class CrashDetector(private val config: CrashConfig) : IssueDetector {
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = buildString {
                append(throwable.stackTraceToString())
                if (config.captureAllThreads) {
                    append("\n\n--- All threads ---\n")
                    Thread.getAllStackTraces().forEach { (t, frames) ->
                        append("\n${t.name} (state=${t.state}):\n")
                        frames.forEach { append("\tat $it\n") }
                    }
                }
            }
            val details = buildMap {
                put("exceptionClass", throwable.javaClass.name)
                throwable.message?.let { put("message", it) }
                if (config.includeDeviceContext) {
                    put("javaVersion", System.getProperty("java.version") ?: "unknown")
                    put("os", "${System.getProperty("os.name")} ${System.getProperty("os.version")}")
                    val runtime = Runtime.getRuntime()
                    put("heapUsedMb", "${(runtime.totalMemory() - runtime.freeMemory()) / 1048576}MB")
                }
            }
            onIssue(
                Issue(
                    id = "${IssueType.CRASH}_${Random.nextLong().toString(16)}",
                    type = IssueType.CRASH,
                    severity = config.severity,
                    message = throwable.javaClass.simpleName + (throwable.message?.let { ": $it" } ?: ""),
                    timestampMs = currentPlatformTimeMs(),
                    stackTrace = stackTrace,
                    threadName = thread.name,
                    details = details
                )
            )
            if (config.chainToPreviousHandler) {
                previousHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    override fun stop() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        previousHandler = null
    }
}
