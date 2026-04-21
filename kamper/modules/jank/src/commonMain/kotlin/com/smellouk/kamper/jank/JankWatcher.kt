package com.smellouk.kamper.jank

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.jank.repository.JankInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class JankWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JankInfoRepository,
    logger: Logger
) : Watcher<JankInfo>(defaultDispatcher, mainDispatcher, repository, logger)
