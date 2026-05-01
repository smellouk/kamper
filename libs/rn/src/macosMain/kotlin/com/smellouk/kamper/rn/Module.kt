package com.smellouk.kamper.rn

import com.smellouk.kamper.api.PerformanceModule
import kotlinx.coroutines.Dispatchers

actual val JsMemoryModule: PerformanceModule<JsMemoryConfig, JsMemoryInfo>
    get() = PerformanceModule(
        config = JsMemoryConfig.DEFAULT,
        performance = JsMemoryPerformance(
            watcher = JsMemoryWatcher(
                defaultDispatcher = Dispatchers.Default,
                mainDispatcher = Dispatchers.Default,
                repository = JsMemoryRepositoryImpl(),
                logger = JsMemoryConfig.DEFAULT.logger
            ),
            logger = JsMemoryConfig.DEFAULT.logger
        )
    )

actual val JsGcModule: PerformanceModule<JsGcConfig, JsGcInfo>
    get() = PerformanceModule(
        config = JsGcConfig.DEFAULT,
        performance = JsGcPerformance(
            watcher = JsGcWatcher(
                defaultDispatcher = Dispatchers.Default,
                mainDispatcher = Dispatchers.Default,
                repository = JsGcRepositoryImpl(),
                logger = JsGcConfig.DEFAULT.logger
            ),
            logger = JsGcConfig.DEFAULT.logger
        )
    )

actual val JsIssueModule: PerformanceModule<JsIssueConfig, JsIssueInfo>
    get() = PerformanceModule(
        config = JsIssueConfig.DEFAULT,
        performance = JsIssuePerformance(
            watcher = JsIssueWatcher(
                defaultDispatcher = Dispatchers.Default,
                mainDispatcher = Dispatchers.Default,
                repository = JsIssueRepositoryImpl(),
                logger = JsIssueConfig.DEFAULT.logger
            ),
            logger = JsIssueConfig.DEFAULT.logger
        )
    )
