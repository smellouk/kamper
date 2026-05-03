package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.gpu.repository.GpuInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class GpuWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: GpuInfoRepository,
    logger: Logger
) : Watcher<GpuInfo>(defaultDispatcher, mainDispatcher, repository, logger)
