package com.smellouk.konitor.jank

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import com.smellouk.konitor.api.Cleanable
import com.smellouk.konitor.api.InfoListener
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.jank.repository.JankFrameTracker

internal class JankPerformance(
    watcher: IWatcher<JankInfo>,
    logger: Logger,
    private val application: Application,
    private val frameTracker: JankFrameTracker,
    private val initialActivity: Activity? = null
) : Performance<JankConfig, IWatcher<JankInfo>, JankInfo>(watcher, logger), Cleanable {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val frameListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        frameTracker.onFrame(metrics)
    }

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) = registerWindow(activity.window)
        override fun onActivityPaused(activity: Activity) = unregisterWindow(activity.window)
        override fun onActivityCreated(activity: Activity, b: Bundle?) = Unit
        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, b: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    override fun initialize(config: JankConfig, listeners: List<InfoListener<JankInfo>>): Boolean {
        if (!super.initialize(config, listeners)) return false
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
        initialActivity?.let { mainHandler.post { registerWindow(it.window) } }
        return true
    }

    private fun registerWindow(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.addOnFrameMetricsAvailableListener(frameListener, mainHandler)
        }
    }

    private fun unregisterWindow(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try { window.removeOnFrameMetricsAvailableListener(frameListener) } catch (_: Exception) {}
        }
    }

    override fun clean() {
        application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
    }
}
