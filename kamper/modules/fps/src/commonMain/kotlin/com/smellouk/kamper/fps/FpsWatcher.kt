package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.fps.repository.FpsInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class FpsWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: FpsInfoRepository,
    logger: Logger
) : Watcher<FpsInfo>(defaultDispatcher, mainDispatcher, repository, logger)
