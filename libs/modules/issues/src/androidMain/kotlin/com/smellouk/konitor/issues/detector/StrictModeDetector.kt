package com.smellouk.konitor.issues.detector

import android.os.Build
import android.os.StrictMode
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import com.smellouk.konitor.issues.StrictModeConfig
import kotlin.random.Random

internal class StrictModeDetector(private val config: StrictModeConfig) : IssueDetector {
    private var previousThreadPolicy: StrictMode.ThreadPolicy? = null
    private var previousVmPolicy: StrictMode.VmPolicy? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        previousThreadPolicy = StrictMode.getThreadPolicy()
        previousVmPolicy = StrictMode.getVmPolicy()

        val threadPolicy = StrictMode.ThreadPolicy.Builder().apply {
            if (config.detectDiskReads) detectDiskReads()
            if (config.detectDiskWrites) detectDiskWrites()
            if (config.detectNetwork) detectNetwork()
            if (config.detectCustomSlowCalls) detectCustomSlowCalls()
            if (config.detectUnbufferedIo && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) detectUnbufferedIo()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                penaltyListener(java.util.concurrent.Executors.newSingleThreadExecutor()) { violation ->
                    onIssue(buildIssue(violation.javaClass.simpleName, violation.stackTraceToString()))
                }
            } else {
                penaltyLog()
            }
        }.build()

        val vmPolicy = StrictMode.VmPolicy.Builder().apply {
            if (config.detectActivityLeaks) detectActivityLeaks()
            if (config.detectLeakedClosableObjects) detectLeakedClosableObjects()
            if (config.detectLeakedSqliteObjects) detectLeakedSqlLiteObjects()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                penaltyListener(java.util.concurrent.Executors.newSingleThreadExecutor()) { violation ->
                    onIssue(buildIssue(violation.javaClass.simpleName, violation.stackTraceToString()))
                }
            } else {
                penaltyLog()
            }
        }.build()

        StrictMode.setThreadPolicy(threadPolicy)
        StrictMode.setVmPolicy(vmPolicy)
    }

    override fun stop() {
        previousThreadPolicy?.let { StrictMode.setThreadPolicy(it) }
        previousVmPolicy?.let { StrictMode.setVmPolicy(it) }
        previousThreadPolicy = null
        previousVmPolicy = null
    }

    private fun buildIssue(violationType: String, stackTrace: String) = Issue(
        id = "${IssueType.STRICT_VIOLATION}_${Random.nextLong().toString(16)}",
        type = IssueType.STRICT_VIOLATION,
        severity = config.severity,
        message = "StrictMode violation: $violationType",
        timestampMs = currentPlatformTimeMs(),
        stackTrace = stackTrace,
        details = mapOf("violationType" to violationType)
    )
}
