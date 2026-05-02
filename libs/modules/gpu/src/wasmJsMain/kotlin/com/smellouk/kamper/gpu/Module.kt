package com.smellouk.kamper.gpu

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.gpu.repository.GpuInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val GpuModule: PerformanceModule<GpuConfig, GpuInfo>
    get() = PerformanceModule(
        config = GpuConfig.DEFAULT,
        performance = createPerformance(GpuConfig.DEFAULT.logger)
    )

@Suppress("FunctionNaming")
fun GpuModule(
    builder: GpuConfig.Builder.() -> Unit
): PerformanceModule<GpuConfig, GpuInfo> = with(GpuConfig.Builder().apply(builder).build()) {
    PerformanceModule(
        config = this,
        performance = createPerformance(logger)
    )
}

private fun createPerformance(
    logger: Logger
): Performance<GpuConfig, IWatcher<GpuInfo>, GpuInfo> = GpuPerformance(
    watcher = GpuWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = GpuInfoRepositoryImpl(),
        logger = logger
    ),
    logger = logger
)
