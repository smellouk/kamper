package com.smellouk.kamper.gpu

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance

internal class GpuPerformance(
    watcher: IWatcher<GpuInfo>,
    logger: Logger
) : Performance<GpuConfig, IWatcher<GpuInfo>, GpuInfo>(watcher, logger)
