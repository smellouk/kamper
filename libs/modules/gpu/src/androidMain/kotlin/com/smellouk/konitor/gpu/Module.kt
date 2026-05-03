package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.gpu.repository.GpuInfoRepositoryImpl
import com.smellouk.konitor.gpu.repository.source.DevfreqGpuInfoSource
import com.smellouk.konitor.gpu.repository.source.FdinfoGpuSource
import com.smellouk.konitor.gpu.repository.source.KgslGpuInfoSource
import com.smellouk.konitor.gpu.repository.source.PixelGpuInfoSource
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
