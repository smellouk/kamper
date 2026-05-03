package com.smellouk.konitor.jank

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.IWatcher

internal class JankPerformance(
    watcher: IWatcher<JankInfo>,
    logger: Logger
) : Performance<JankConfig, IWatcher<JankInfo>, JankInfo>(watcher, logger)
