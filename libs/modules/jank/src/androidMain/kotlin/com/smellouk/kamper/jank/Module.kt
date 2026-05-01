package com.smellouk.kamper.jank

import android.app.Activity
import android.app.Application
import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.jank.repository.JankFrameTracker
import com.smellouk.kamper.jank.repository.JankInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val JankModule: PerformanceModule<JankConfig, JankInfo>
    get() = throw IllegalStateException("Use JankModule(application) on Android")

@Suppress("FunctionNaming")
fun JankModule(
    application: Application,
    initialActivity: Activity? = null,
    builder: JankConfig.Builder.() -> Unit = {}
): PerformanceModule<JankConfig, JankInfo> = with(JankConfig.Builder().apply(builder).build()) {
    val tracker = JankFrameTracker()
    PerformanceModule(
        config = this,
        performance = JankPerformance(
            watcher = JankWatcher(
                defaultDispatcher = Dispatchers.Default,
                mainDispatcher = Dispatchers.Main,
                repository = JankInfoRepositoryImpl(tracker, jankThresholdMs),
                logger = logger
            ),
            logger = logger,
            application = application,
            frameTracker = tracker,
            initialActivity = initialActivity
        )
    )
}
