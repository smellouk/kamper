package com.smellouk.kamper.memory

import android.content.Context
import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.memory.repository.MemoryInfoMapper
import com.smellouk.kamper.memory.repository.MemoryInfoRepositoryImpl
import com.smellouk.kamper.memory.repository.source.MemoryInfoSource
import kotlinx.coroutines.Dispatchers

@KamperDslMarker
@Suppress("FunctionNaming")
fun MemoryModule(
    context: Context,
    builder: MemoryConfig.Builder.() -> Unit = MemoryConfig.Builder.DEFAULT_ACTION
): PerformanceModule<MemoryConfig, MemoryInfo> =
    with(MemoryConfig.Builder.DEFAULT.apply(builder).build()) {
        PerformanceModule(
            config = this,
            performance = createPerformance(context, logger)
        )
    }

private fun createPerformance(
    context: Context,
    logger: Logger
): Performance<MemoryConfig, Watcher<MemoryInfo>, MemoryInfo> = MemoryPerformance(
    MemoryWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        repository = MemoryInfoRepositoryImpl(
            memorySource = MemoryInfoSource(context, logger),
            memoryInfoMapper = MemoryInfoMapper(),
        ),
        logger = logger
    )
)
