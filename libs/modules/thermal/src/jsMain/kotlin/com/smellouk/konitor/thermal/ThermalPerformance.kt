package com.smellouk.konitor.thermal

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.IWatcher

internal class ThermalPerformance(
    watcher: IWatcher<ThermalInfo>,
    logger: Logger
) : Performance<ThermalConfig, IWatcher<ThermalInfo>, ThermalInfo>(watcher, logger)
