package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.cpu.repository.CpuInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class CpuWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: CpuInfoRepository,
    logger: Logger
) : Watcher<CpuInfo>(defaultDispatcher, mainDispatcher, repository, logger)
