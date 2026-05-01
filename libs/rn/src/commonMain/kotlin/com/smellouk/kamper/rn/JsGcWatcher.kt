package com.smellouk.kamper.rn

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import kotlinx.coroutines.CoroutineDispatcher

internal class JsGcWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JsGcRepository,
    logger: Logger
) : Watcher<JsGcInfo>(defaultDispatcher, mainDispatcher, repository, logger)
