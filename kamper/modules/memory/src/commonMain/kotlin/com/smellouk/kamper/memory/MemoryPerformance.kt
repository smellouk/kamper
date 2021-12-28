package com.smellouk.kamper.memory

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher

internal class MemoryPerformance(
    watcher: MemoryWatcher,
    logger: Logger
) : Performance<MemoryConfig, Watcher<MemoryInfo>, MemoryInfo>(watcher, logger)
