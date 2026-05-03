package com.smellouk.konitor.fps

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.fps.repository.FpsInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class FpsWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: FpsInfoRepository,
    logger: Logger
) : Watcher<FpsInfo>(defaultDispatcher, mainDispatcher, repository, logger)
