package com.smellouk.kamper.issues.detector

import com.smellouk.kamper.issues.CrashConfig
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import platform.Foundation.NSException
import platform.Foundation.NSSetUncaughtExceptionHandler
import kotlin.random.Random

@OptIn(ExperimentalForeignApi::class)
internal class CrashDetector(private val config: CrashConfig) : IssueDetector {
    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        pendingOnIssue = onIssue
        pendingConfig = config
        NSSetUncaughtExceptionHandler(
            staticCFunction { exception: NSException? ->
                val ex = exception ?: return@staticCFunction
                val cfg = pendingConfig ?: return@staticCFunction
                pendingOnIssue?.invoke(
                    Issue(
                        id = "${IssueType.CRASH}_${Random.nextLong().toString(16)}",
                        type = IssueType.CRASH,
                        severity = cfg.severity,
                        message = "${ex.name}: ${ex.reason.orEmpty()}",
                        timestampMs = currentPlatformTimeMs(),
                        stackTrace = ex.callStackSymbols.joinToString("\n"),
                        details = mapOf("exceptionName" to (ex.name ?: ""))
                    )
                )
            }
        )
    }

    override fun stop() {
        NSSetUncaughtExceptionHandler(null)
        pendingOnIssue = null
        pendingConfig = null
    }

    companion object {
        private var pendingOnIssue: ((Issue) -> Unit)? = null
        private var pendingConfig: CrashConfig? = null
    }
}
