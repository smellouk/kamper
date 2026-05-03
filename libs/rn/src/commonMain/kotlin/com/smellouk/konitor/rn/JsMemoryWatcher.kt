package com.smellouk.konitor.rn

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import kotlinx.coroutines.CoroutineDispatcher

internal class JsMemoryWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JsMemoryRepository,
    logger: Logger
) : Watcher<JsMemoryInfo>(defaultDispatcher, mainDispatcher, repository, logger)
