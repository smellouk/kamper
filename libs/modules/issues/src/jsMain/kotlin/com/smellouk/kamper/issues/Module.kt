package com.smellouk.kamper.issues

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.issues.detector.IssueDetector
import com.smellouk.kamper.issues.detector.SlowSpanDetector

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
