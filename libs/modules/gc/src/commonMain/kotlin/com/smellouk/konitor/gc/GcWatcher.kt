package com.smellouk.konitor.gc

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.gc.repository.GcInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class GcWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: GcInfoRepository,
    logger: Logger
) : Watcher<GcInfo>(defaultDispatcher, mainDispatcher, repository, logger)
