package com.smellouk.konitor.issues.detector

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import com.smellouk.konitor.issues.SlowStartConfig
import kotlin.random.Random

internal class SlowStartDetector(
    private val config: SlowStartConfig,
    private val application: Application
) : IssueDetector {
    private var moduleStartMs = 0L
    private var callbacks: Application.ActivityLifecycleCallbacks? = null

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        moduleStartMs = currentPlatformTimeMs()
        var isColdStart = true

        callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                val elapsed = currentPlatformTimeMs() - moduleStartMs
                if (isColdStart) {
                    isColdStart = false
                    if (elapsed > config.coldStartThresholdMs) {
                        onIssue(buildIssue(IssueType.SLOW_COLD_START, elapsed, config.coldStartThresholdMs, config))
                    }
                } else if (elapsed > config.hotStartThresholdMs) {
                    onIssue(buildIssue(IssueType.SLOW_HOT_START, elapsed, config.hotStartThresholdMs, config))
                }
                moduleStartMs = currentPlatformTimeMs()
            }

            override fun onActivityPaused(activity: Activity) {
                moduleStartMs = currentPlatformTimeMs()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }.also { application.registerActivityLifecycleCallbacks(it) }
    }

    override fun stop() {
        callbacks?.let { application.unregisterActivityLifecycleCallbacks(it) }
        callbacks = null
    }

    private fun buildIssue(type: IssueType, elapsed: Long, threshold: Long, config: SlowStartConfig) = Issue(
        id = "${type}_${Random.nextLong().toString(16)}",
        type = type,
        severity = config.severity,
        message = "${type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }} took ${elapsed}ms (threshold: ${threshold}ms)",
        timestampMs = currentPlatformTimeMs(),
        durationMs = elapsed,
        details = mapOf("threshold" to "${threshold}ms")
    )
}
