package com.smellouk.konitor.issues.detector

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import com.smellouk.konitor.issues.DroppedFramesConfig
import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import kotlin.random.Random

internal class DroppedFramesDetector(
    private val config: DroppedFramesConfig,
    private val application: Application
) : IssueDetector {
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null
    private var lastReportMs = 0L
    private var consecutiveDrops = 0

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
            private val frameListeners = mutableMapOf<Activity, Window.OnFrameMetricsAvailableListener>()

            override fun onActivityStarted(activity: Activity) {
                val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
                    val totalMs = metrics.getMetric(FrameMetrics.TOTAL_DURATION) / 1_000_000L
                    if (totalMs > config.frameThresholdMs) {
                        consecutiveDrops++
                        if (consecutiveDrops >= config.consecutiveFramesThreshold) {
                            val now = currentPlatformTimeMs()
                            if (now - lastReportMs >= config.cooldownMs) {
                                lastReportMs = now
                                consecutiveDrops = 0
                                onIssue(
                                    Issue(
                                        id = "${IssueType.DROPPED_FRAME}_${Random.nextLong().toString(16)}",
                                        type = IssueType.DROPPED_FRAME,
                                        severity = config.severity,
                                        message = "Frame took ${totalMs}ms (threshold: ${config.frameThresholdMs}ms)",
                                        timestampMs = now,
                                        durationMs = totalMs,
                                        details = mapOf(
                                            "threshold" to "${config.frameThresholdMs}ms",
                                            "consecutiveThreshold" to "${config.consecutiveFramesThreshold}"
                                        )
                                    )
                                )
                            }
                        }
                    } else {
                        consecutiveDrops = 0
                    }
                }
                activity.window.addOnFrameMetricsAvailableListener(listener, Handler(Looper.getMainLooper()))
                frameListeners[activity] = listener
            }

            override fun onActivityStopped(activity: Activity) {
                frameListeners.remove(activity)?.let { activity.window.removeOnFrameMetricsAvailableListener(it) }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        }.also { application.registerActivityLifecycleCallbacks(it) }
    }

    override fun stop() {
        lifecycleCallbacks?.let { application.unregisterActivityLifecycleCallbacks(it) }
        lifecycleCallbacks = null
    }
}
