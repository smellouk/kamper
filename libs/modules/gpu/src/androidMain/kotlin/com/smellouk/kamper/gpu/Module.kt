package com.smellouk.kamper.gpu

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.gpu.repository.GpuInfoRepositoryImpl
import com.smellouk.kamper.gpu.repository.source.DevfreqGpuInfoSource
import com.smellouk.kamper.gpu.repository.source.FdinfoGpuSource
import com.smellouk.kamper.gpu.repository.source.KgslGpuInfoSource
import com.smellouk.kamper.gpu.repository.source.PixelGpuInfoSource
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
        mainDispatcher = Dispatchers.Main,
        repository = GpuInfoRepositoryImpl(
            pixelSource = PixelGpuInfoSource(logger),
            kgslSource = KgslGpuInfoSource(logger),
            devfreqSource = DevfreqGpuInfoSource(logger),
            fdinfoSource = FdinfoGpuSource(),
            logger = logger
        ),
        logger = logger
    ),
    logger = logger
)
