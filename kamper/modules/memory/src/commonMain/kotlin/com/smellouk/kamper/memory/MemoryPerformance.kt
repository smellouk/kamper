package com.smellouk.kamper.memory

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.IWatcher

internal class MemoryPerformance(
    watcher: IWatcher<MemoryInfo>,
    logger: Logger
) : Performance<MemoryConfig, IWatcher<MemoryInfo>, MemoryInfo>(watcher, logger)
