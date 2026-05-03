package com.smellouk.konitor.cpu

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.IWatcher

internal class CpuPerformance(
    watcher: IWatcher<CpuInfo>,
    logger: Logger
) : Performance<CpuConfig, IWatcher<CpuInfo>, CpuInfo>(watcher, logger)
