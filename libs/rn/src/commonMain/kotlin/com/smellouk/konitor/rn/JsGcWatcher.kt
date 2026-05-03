package com.smellouk.konitor.rn

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import kotlinx.coroutines.CoroutineDispatcher

internal class JsGcWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JsGcRepository,
    logger: Logger
) : Watcher<JsGcInfo>(defaultDispatcher, mainDispatcher, repository, logger)
