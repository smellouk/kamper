package com.smellouk.kamper.memory

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.memory.repository.MemoryInfoMapper
import com.smellouk.kamper.memory.repository.MemoryInfoRepositoryImpl
import com.smellouk.kamper.memory.repository.source.JvmMemoryInfoSource
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
            memoryInfoSource = JvmMemoryInfoSource(),
            memoryInfoMapper = MemoryInfoMapper()
        ),
        logger = logger
    ),
    logger = logger
)
