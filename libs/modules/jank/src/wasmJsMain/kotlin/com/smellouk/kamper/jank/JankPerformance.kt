package com.smellouk.kamper.jank

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.IWatcher

internal class JankPerformance(
    watcher: IWatcher<JankInfo>,
    logger: Logger
) : Performance<JankConfig, IWatcher<JankInfo>, JankInfo>(watcher, logger)
