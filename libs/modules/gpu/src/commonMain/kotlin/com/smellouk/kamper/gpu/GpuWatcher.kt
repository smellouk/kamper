package com.smellouk.kamper.gpu

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.gpu.repository.GpuInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class GpuWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: GpuInfoRepository,
    logger: Logger
) : Watcher<GpuInfo>(defaultDispatcher, mainDispatcher, repository, logger)
