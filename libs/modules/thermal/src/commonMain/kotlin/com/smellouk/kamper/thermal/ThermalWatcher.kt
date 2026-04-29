package com.smellouk.kamper.thermal

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.thermal.repository.ThermalInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class ThermalWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: ThermalInfoRepository,
    logger: Logger
) : Watcher<ThermalInfo>(defaultDispatcher, mainDispatcher, repository, logger)
