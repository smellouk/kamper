package com.smellouk.konitor.issues

import android.app.Application
import android.content.Context
import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.issues.detector.AnrDetector
import com.smellouk.konitor.issues.detector.CrashDetector
import com.smellouk.konitor.issues.detector.DroppedFramesDetector
import com.smellouk.konitor.issues.detector.IssueDetector
import com.smellouk.konitor.issues.detector.MemoryPressureDetector
import com.smellouk.konitor.issues.detector.SlowSpanDetector
import com.smellouk.konitor.issues.detector.SlowStartDetector
import com.smellouk.konitor.issues.detector.StrictModeDetector

actual val IssuesModule: PerformanceModule<IssuesConfig, IssueInfo>
    get() = error("On Android use IssuesModule(context) { ... }")

@Suppress("FunctionNaming")
fun IssuesModule(
    context: Context,
    anr: AnrConfig = AnrConfig(),
    slowStart: SlowStartConfig = SlowStartConfig(),
    strictMode: StrictModeConfig = StrictModeConfig(),
    builder: IssuesConfig.Builder.() -> Unit = {}
): PerformanceModule<IssuesConfig, IssueInfo> {
    val config = IssuesConfig.Builder().apply(builder).build()
    val application = context.applicationContext as Application
    return PerformanceModule(
        config = config,
        performance = IssuesPerformance(
            issuesWatcher = IssuesWatcher(
                detectors = buildDetectors(config, anr, slowStart, strictMode, application),
                config = config
            ),
            logger = config.logger
        )
    )
}

private fun buildDetectors(
    config: IssuesConfig,
    anr: AnrConfig,
    slowStart: SlowStartConfig,
    strictMode: StrictModeConfig,
    application: Application
): List<IssueDetector> = buildList {
    if (config.slowSpan.isEnabled) add(SlowSpanDetector(config.slowSpan))
    if (anr.isEnabled) add(AnrDetector(anr))
    if (slowStart.isEnabled) add(SlowStartDetector(slowStart, application))
    if (config.droppedFrames.isEnabled) add(DroppedFramesDetector(config.droppedFrames, application))
    if (config.crash.isEnabled) add(CrashDetector(config.crash, application))
    if (config.memoryPressure.isEnabled) add(MemoryPressureDetector(config.memoryPressure, application))
    if (strictMode.isEnabled) add(StrictModeDetector(strictMode))
}
