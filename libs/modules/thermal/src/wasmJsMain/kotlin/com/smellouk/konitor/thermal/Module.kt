package com.smellouk.konitor.thermal

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.thermal.repository.ThermalInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val ThermalModule: PerformanceModule<ThermalConfig, ThermalInfo>
    get() = PerformanceModule(
        config = ThermalConfig.DEFAULT,
        performance = createPerformance(ThermalConfig.DEFAULT.logger)
    )

private fun createPerformance(logger: Logger) = ThermalPerformance(
    watcher = ThermalWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = ThermalInfoRepositoryImpl(),
        logger = logger
    ),
    logger = logger
)
