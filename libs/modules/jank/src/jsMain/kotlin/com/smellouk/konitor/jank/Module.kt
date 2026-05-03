package com.smellouk.konitor.jank

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.jank.repository.JankInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val JankModule: PerformanceModule<JankConfig, JankInfo>
    get() = PerformanceModule(
        config = JankConfig.DEFAULT,
        performance = createPerformance(JankConfig.DEFAULT.logger)
    )

private fun createPerformance(logger: Logger) = JankPerformance(
    watcher = JankWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = JankInfoRepositoryImpl(),
        logger = logger
    ),
    logger = logger
)
