package com.smellouk.konitor.network

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.network.repository.NetworkInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class NetworkWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: NetworkInfoRepository,
    logger: Logger
) : Watcher<NetworkInfo>(defaultDispatcher, mainDispatcher, repository, logger)
