package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.IWatcher

internal class CpuPerformance(
    watcher: IWatcher<CpuInfo>,
    logger: Logger
) : Performance<CpuConfig, IWatcher<CpuInfo>, CpuInfo>(watcher, logger)
