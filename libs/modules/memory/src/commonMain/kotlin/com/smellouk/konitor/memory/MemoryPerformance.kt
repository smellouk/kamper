package com.smellouk.konitor.memory

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.IWatcher

internal class MemoryPerformance(
    watcher: IWatcher<MemoryInfo>,
    logger: Logger
) : Performance<MemoryConfig, IWatcher<MemoryInfo>, MemoryInfo>(watcher, logger)
