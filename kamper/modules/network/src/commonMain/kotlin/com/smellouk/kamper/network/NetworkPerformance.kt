package com.smellouk.kamper.network

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher

internal class NetworkPerformance(
    watcher: NetworkWatcher,
    logger: Logger
) : Performance<NetworkConfig, Watcher<NetworkInfo>, NetworkInfo>(watcher, logger)
