package com.smellouk.konitor.network

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.IWatcher

internal class NetworkPerformance(
    watcher: IWatcher<NetworkInfo>,
    logger: Logger
) : Performance<NetworkConfig, IWatcher<NetworkInfo>, NetworkInfo>(watcher, logger)
