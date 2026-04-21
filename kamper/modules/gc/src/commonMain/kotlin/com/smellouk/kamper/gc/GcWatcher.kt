package com.smellouk.kamper.gc

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.gc.repository.GcInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class GcWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: GcInfoRepository,
    logger: Logger
) : Watcher<GcInfo>(defaultDispatcher, mainDispatcher, repository, logger)
