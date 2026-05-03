package com.smellouk.konitor.issues

import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.issues.detector.AnrDetector
import com.smellouk.konitor.issues.detector.CrashDetector
import com.smellouk.konitor.issues.detector.IssueDetector
import com.smellouk.konitor.issues.detector.SlowSpanDetector

actual val IssuesModule: PerformanceModule<IssuesConfig, IssueInfo>
    get() = IssuesModule()

@Suppress("FunctionNaming")
fun IssuesModule(
    anr: AnrConfig = AnrConfig(),
    builder: IssuesConfig.Builder.() -> Unit = {}
): PerformanceModule<IssuesConfig, IssueInfo> {
    val config = IssuesConfig.Builder().apply(builder).build()
    return PerformanceModule(
        config = config,
        performance = IssuesPerformance(
            issuesWatcher = IssuesWatcher(
                detectors = buildDetectors(config, anr),
                config = config
            ),
            logger = config.logger
        )
    )
}

private fun buildDetectors(config: IssuesConfig, anr: AnrConfig): List<IssueDetector> = buildList {
    if (config.slowSpan.isEnabled) add(SlowSpanDetector(config.slowSpan))
    if (anr.isEnabled) add(AnrDetector(anr))
    if (config.crash.isEnabled) add(CrashDetector(config.crash))
}
