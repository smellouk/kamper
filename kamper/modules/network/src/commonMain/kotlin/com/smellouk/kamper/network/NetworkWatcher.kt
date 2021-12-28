package com.smellouk.kamper.network

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.network.repository.NetworkInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class NetworkWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: NetworkInfoRepository,
    logger: Logger
) : Watcher<NetworkInfo>(defaultDispatcher, mainDispatcher, repository, logger)
