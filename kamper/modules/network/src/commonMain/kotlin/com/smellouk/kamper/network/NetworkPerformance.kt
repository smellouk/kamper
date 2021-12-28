package com.smellouk.kamper.network

import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher

internal class NetworkPerformance(
    watcher: NetworkWatcher
) : Performance<NetworkConfig, Watcher<NetworkInfo>, NetworkInfo>(watcher)
