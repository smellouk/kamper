package com.smellouk.konitor.jank

import android.app.Activity
import android.app.Application
import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.jank.repository.JankFrameTracker
import com.smellouk.konitor.jank.repository.JankInfoRepositoryImpl
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
