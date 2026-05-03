package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance

internal class GpuPerformance(
    watcher: IWatcher<GpuInfo>,
    logger: Logger
) : Performance<GpuConfig, IWatcher<GpuInfo>, GpuInfo>(watcher, logger)
