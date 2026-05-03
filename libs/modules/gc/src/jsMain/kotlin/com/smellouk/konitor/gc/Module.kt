package com.smellouk.konitor.gc

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.gc.repository.GcInfoRepositoryImpl
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
        repository = GcInfoRepositoryImpl(),
        logger = logger
    ),
    logger = logger
)
