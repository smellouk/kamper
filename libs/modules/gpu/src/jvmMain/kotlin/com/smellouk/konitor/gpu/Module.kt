package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.gpu.repository.GpuInfoRepositoryImpl
import com.smellouk.konitor.gpu.repository.source.OshiGpuInfoSource
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
        repository = GpuInfoRepositoryImpl(source = OshiGpuInfoSource()),
        logger = logger
    ),
    logger = logger
)
