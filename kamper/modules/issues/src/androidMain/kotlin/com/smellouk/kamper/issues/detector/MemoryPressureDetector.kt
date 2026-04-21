package com.smellouk.kamper.issues.detector

import android.app.ActivityManager
import android.content.Context
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.MemoryPressureConfig
import com.smellouk.kamper.issues.Severity
import kotlin.random.Random

internal class MemoryPressureDetector(
    private val config: MemoryPressureConfig,
    private val context: Context
) : IssueDetector {
    @Volatile private var running = false
    private var thread: Thread? = null
    private var lastReportMs = 0L

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        val runtime = Runtime.getRuntime()
        running = true

        thread = Thread {
            while (running && !Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(config.checkIntervalMs)
                    activityManager.getMemoryInfo(memInfo)
                    val heapUsed = runtime.totalMemory() - runtime.freeMemory()
                    val heapMax = runtime.maxMemory()
                    val heapRatio = heapUsed.toFloat() / heapMax.toFloat()
                    val now = currentPlatformTimeMs()

                    val (type, severity, threshold) = when {
                        heapRatio >= config.criticalThresholdPercent || memInfo.lowMemory ->
                            Triple(IssueType.NEAR_OOM, Severity.CRITICAL, config.criticalThresholdPercent)
                        heapRatio >= config.warningThresholdPercent ->
                            Triple(IssueType.MEMORY_PRESSURE, Severity.WARNING, config.warningThresholdPercent)
                        else -> continue
                    }

                    if (now - lastReportMs >= config.cooldownMs) {
                        lastReportMs = now
                        onIssue(
                            Issue(
                                id = "${type}_${Random.nextLong().toString(16)}",
                                type = type,
                                severity = severity,
                                message = "Heap at ${(heapRatio * 100).toInt()}% of max (threshold: ${(threshold * 100).toInt()}%)",
                                timestampMs = now,
                                details = mapOf(
                                    "heapUsedMb" to "${heapUsed / 1048576}MB",
                                    "heapMaxMb" to "${heapMax / 1048576}MB",
                                    "lowMemory" to "${memInfo.lowMemory}"
                                )
                            )
                        )
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }.also {
            it.name = "Kamper-MemoryPressure"
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        running = false
        thread?.interrupt()
        thread = null
    }
}
