package com.smellouk.kamper.gc

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.gc.repository.GcInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val GcModule: PerformanceModule<GcConfig, GcInfo>
    get() = PerformanceModule(
        config = GcConfig.DEFAULT,
        performance = createPerformance(GcConfig.DEFAULT.logger)
    )

private fun createPerformance(logger: Logger) = GcPerformance(
    watcher = GcWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = GcInfoRepositoryImpl(logger),
        logger = logger
    ),
    logger = logger
)
