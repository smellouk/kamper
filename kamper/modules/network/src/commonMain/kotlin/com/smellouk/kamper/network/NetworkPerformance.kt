package com.smellouk.kamper.network

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.IWatcher

internal class NetworkPerformance(
    watcher: IWatcher<NetworkInfo>,
    logger: Logger
) : Performance<NetworkConfig, IWatcher<NetworkInfo>, NetworkInfo>(watcher, logger)
