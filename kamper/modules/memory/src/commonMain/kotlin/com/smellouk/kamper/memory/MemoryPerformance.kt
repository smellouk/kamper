package com.smellouk.kamper.memory

import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher

internal class MemoryPerformance(
    watcher: MemoryWatcher
) : Performance<MemoryConfig, Watcher<MemoryInfo>, MemoryInfo>(watcher)
