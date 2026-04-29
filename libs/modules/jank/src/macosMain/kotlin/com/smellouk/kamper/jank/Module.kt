package com.smellouk.kamper.jank

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.jank.repository.JankInfoRepositoryImpl
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
