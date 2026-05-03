package com.smellouk.konitor.cpu

import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.cpu.repository.CpuInfoMapper
import com.smellouk.konitor.cpu.repository.CpuInfoRepositoryImpl
import com.smellouk.konitor.cpu.repository.source.JvmCpuInfoSource
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
