package com.smellouk.konitor.jank

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.jank.repository.JankInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class JankWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JankInfoRepository,
    logger: Logger
) : Watcher<JankInfo>(defaultDispatcher, mainDispatcher, repository, logger)
