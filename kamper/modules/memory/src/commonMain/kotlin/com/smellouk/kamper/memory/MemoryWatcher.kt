package com.smellouk.kamper.memory

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.memory.repository.MemoryInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class MemoryWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: MemoryInfoRepository,
    logger: Logger
) : Watcher<MemoryInfo>(defaultDispatcher, mainDispatcher, repository, logger)
