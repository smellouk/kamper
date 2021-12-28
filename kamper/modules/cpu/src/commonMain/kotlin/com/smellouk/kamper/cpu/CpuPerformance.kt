package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher

internal class CpuPerformance(
    watcher: CpuWatcher,
    logger: Logger
) : Performance<CpuConfig, Watcher<CpuInfo>, CpuInfo>(watcher, logger)
