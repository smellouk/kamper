package com.smellouk.konitor.gc

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.IWatcher

internal class GcPerformance(
    watcher: IWatcher<GcInfo>,
    logger: Logger
) : Performance<GcConfig, IWatcher<GcInfo>, GcInfo>(watcher, logger)
