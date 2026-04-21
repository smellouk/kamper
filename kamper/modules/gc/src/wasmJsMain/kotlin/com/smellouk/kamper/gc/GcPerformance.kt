package com.smellouk.kamper.gc

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.IWatcher

internal class GcPerformance(
    watcher: IWatcher<GcInfo>,
    logger: Logger
) : Performance<GcConfig, IWatcher<GcInfo>, GcInfo>(watcher, logger)
