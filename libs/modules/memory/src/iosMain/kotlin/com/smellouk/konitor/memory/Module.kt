package com.smellouk.konitor.memory

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.memory.repository.MemoryInfoMapper
import com.smellouk.konitor.memory.repository.MemoryInfoRepositoryImpl
import com.smellouk.konitor.memory.repository.source.IosMemoryInfoSource
import kotlinx.coroutines.Dispatchers

@Suppress("FunctionNaming")
fun MemoryModule(
    builder: MemoryConfig.Builder.() -> Unit = MemoryConfig.Builder.DEFAULT_ACTION
): PerformanceModule<MemoryConfig, MemoryInfo> =
    with(MemoryConfig.Builder.DEFAULT.apply(builder).build()) {
        PerformanceModule(
            config = this,
            performance = createPerformance(logger)
        )
    }

private fun createPerformance(
    logger: Logger
): Performance<MemoryConfig, IWatcher<MemoryInfo>, MemoryInfo> = MemoryPerformance(
    watcher = MemoryWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = MemoryInfoRepositoryImpl(
            memoryInfoSource = IosMemoryInfoSource(),
            memoryInfoMapper = MemoryInfoMapper()
        ),
        logger = logger
    ),
    logger = logger
)
