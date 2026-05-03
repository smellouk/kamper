package com.smellouk.konitor.issues

import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.issues.detector.IssueDetector
import com.smellouk.konitor.issues.detector.SlowSpanDetector

actual val IssuesModule: PerformanceModule<IssuesConfig, IssueInfo>
    get() = IssuesModule()

@Suppress("FunctionNaming")
fun IssuesModule(
    builder: IssuesConfig.Builder.() -> Unit = {}
): PerformanceModule<IssuesConfig, IssueInfo> {
    val config = IssuesConfig.Builder().apply(builder).build()
    return PerformanceModule(
        config = config,
        performance = IssuesPerformance(
            issuesWatcher = IssuesWatcher(
                detectors = buildDetectors(config),
                config = config
            ),
            logger = config.logger
        )
    )
}

private fun buildDetectors(config: IssuesConfig): List<IssueDetector> = buildList {
    if (config.slowSpan.isEnabled) add(SlowSpanDetector(config.slowSpan))
}
