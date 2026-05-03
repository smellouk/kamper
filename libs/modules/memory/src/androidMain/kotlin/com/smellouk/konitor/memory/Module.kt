package com.smellouk.konitor.memory

import android.content.Context
import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.memory.repository.MemoryInfoMapper
import com.smellouk.konitor.memory.repository.MemoryInfoRepositoryImpl
import com.smellouk.konitor.memory.repository.source.MemoryInfoSource
import kotlinx.coroutines.Dispatchers

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
): Performance<MemoryConfig, IWatcher<MemoryInfo>, MemoryInfo> = MemoryPerformance(
    watcher = MemoryWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        repository = MemoryInfoRepositoryImpl(
            memoryInfoSource = MemoryInfoSource(context, logger),
            memoryInfoMapper = MemoryInfoMapper(),
        ),
        logger = logger
    ),
    logger = logger
)
