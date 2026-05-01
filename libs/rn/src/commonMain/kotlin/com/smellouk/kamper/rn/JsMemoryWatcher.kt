package com.smellouk.kamper.rn

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import kotlinx.coroutines.CoroutineDispatcher

internal class JsMemoryWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JsMemoryRepository,
    logger: Logger
) : Watcher<JsMemoryInfo>(defaultDispatcher, mainDispatcher, repository, logger)
