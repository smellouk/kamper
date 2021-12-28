package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher

internal class CpuPerformance(
    watcher: CpuWatcher,
) : Performance<CpuConfig, Watcher<CpuInfo>, CpuInfo>(watcher)
