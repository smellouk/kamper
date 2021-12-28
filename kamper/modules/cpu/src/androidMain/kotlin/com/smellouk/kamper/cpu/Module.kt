package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.cpu.repository.CpuInfoMapper
import com.smellouk.kamper.cpu.repository.CpuInfoRepositoryImpl
import com.smellouk.kamper.cpu.repository.source.ProcCpuInfoSource
import com.smellouk.kamper.cpu.repository.source.ShellCpuInfoSource
import kotlinx.coroutines.Dispatchers

actual val CpuModule: PerformanceModule<CpuConfig, CpuInfo> = PerformanceModule(
    config = CpuConfig.DEFAULT,
    performance = createPerformance(CpuConfig.DEFAULT.logger)
)

@KamperDslMarker
@Suppress("FunctionNaming")
fun CpuModule(
    builder: CpuConfig.Builder.() -> Unit
): PerformanceModule<CpuConfig, CpuInfo> = with(CpuConfig.Builder.apply(builder).build()) {
    PerformanceModule(
        config = this,
        performance = createPerformance(logger)
    )
}

private fun createPerformance(
    logger: Logger
): Performance<CpuConfig, Watcher<CpuInfo>, CpuInfo> = CpuPerformance(
    watcher = CpuWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        repository = CpuInfoRepositoryImpl(
            cpuInfoMapper = CpuInfoMapper(),
            procCpuInfoRawSource = ProcCpuInfoSource(),
            shellCpuInfoRawSource = ShellCpuInfoSource(logger),
        ),
        logger = logger
    ),
    logger = logger
)
