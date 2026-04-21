package com.smellouk.kamper.thermal

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.thermal.repository.ThermalInfoRepositoryImpl
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
