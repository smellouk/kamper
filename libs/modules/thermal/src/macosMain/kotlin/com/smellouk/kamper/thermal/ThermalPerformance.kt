package com.smellouk.kamper.thermal

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.IWatcher

internal class ThermalPerformance(
    watcher: IWatcher<ThermalInfo>,
    logger: Logger
) : Performance<ThermalConfig, IWatcher<ThermalInfo>, ThermalInfo>(watcher, logger)
