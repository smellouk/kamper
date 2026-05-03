package com.smellouk.konitor.memory

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.memory.repository.MemoryInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class MemoryWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: MemoryInfoRepository,
    logger: Logger
) : Watcher<MemoryInfo>(defaultDispatcher, mainDispatcher, repository, logger)
