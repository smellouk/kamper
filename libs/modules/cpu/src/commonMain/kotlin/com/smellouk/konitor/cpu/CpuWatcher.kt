package com.smellouk.konitor.cpu

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.cpu.repository.CpuInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class CpuWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: CpuInfoRepository,
    logger: Logger
) : Watcher<CpuInfo>(defaultDispatcher, mainDispatcher, repository, logger)
