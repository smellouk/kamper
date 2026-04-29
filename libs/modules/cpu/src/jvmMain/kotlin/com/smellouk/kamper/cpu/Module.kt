package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.cpu.repository.CpuInfoMapper
import com.smellouk.kamper.cpu.repository.CpuInfoRepositoryImpl
import com.smellouk.kamper.cpu.repository.source.JvmCpuInfoSource
import kotlinx.coroutines.Dispatchers

actual val CpuModule: PerformanceModule<CpuConfig, CpuInfo>
    get() = PerformanceModule(
        config = CpuConfig.DEFAULT,
        performance = createPerformance(CpuConfig.DEFAULT.logger)
    )

@Suppress("FunctionNaming")
fun CpuModule(
    builder: CpuConfig.Builder.() -> Unit
): PerformanceModule<CpuConfig, CpuInfo> = with(CpuConfig.Builder().apply(builder).build()) {
    PerformanceModule(
        config = this,
        performance = createPerformance(logger)
    )
}

private fun createPerformance(
    logger: Logger
): Performance<CpuConfig, IWatcher<CpuInfo>, CpuInfo> = CpuPerformance(
    watcher = CpuWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = CpuInfoRepositoryImpl(
            cpuInfoSource = JvmCpuInfoSource(),
            cpuInfoMapper = CpuInfoMapper()
        ),
        logger = logger
    ),
    logger = logger
)
