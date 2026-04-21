package com.smellouk.kamper.issues

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.issues.detector.AnrDetector
import com.smellouk.kamper.issues.detector.CrashDetector
import com.smellouk.kamper.issues.detector.IssueDetector
import com.smellouk.kamper.issues.detector.SlowSpanDetector

actual val IssuesModule: PerformanceModule<IssuesConfig, IssueInfo>
    get() = IssuesModule()

@KamperDslMarker
@Suppress("FunctionNaming")
fun IssuesModule(
    anr: AnrConfig = AnrConfig(),
    slowStart: SlowStartConfig = SlowStartConfig(),
    builder: IssuesConfig.Builder.() -> Unit = {}
): PerformanceModule<IssuesConfig, IssueInfo> {
    val config = IssuesConfig.Builder.apply(builder).build()
    return PerformanceModule(
        config = config,
        performance = IssuesPerformance(
            issuesWatcher = IssuesWatcher(detectors = buildDetectors(config, anr)),
            logger = config.logger
        )
    )
}

private fun buildDetectors(config: IssuesConfig, anr: AnrConfig): List<IssueDetector> = buildList {
    if (config.slowSpan.isEnabled) add(SlowSpanDetector(config.slowSpan))
    if (anr.isEnabled) add(AnrDetector(anr))
    if (config.crash.isEnabled) add(CrashDetector(config.crash))
}
