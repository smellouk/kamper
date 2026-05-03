package com.smellouk.konitor.thermal

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.thermal.repository.ThermalInfoRepository
import kotlinx.coroutines.CoroutineDispatcher

internal class ThermalWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: ThermalInfoRepository,
    logger: Logger
) : Watcher<ThermalInfo>(defaultDispatcher, mainDispatcher, repository, logger)
