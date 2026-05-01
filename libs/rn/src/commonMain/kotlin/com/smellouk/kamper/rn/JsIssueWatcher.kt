package com.smellouk.kamper.rn

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Watcher
import kotlinx.coroutines.CoroutineDispatcher

internal class JsIssueWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JsIssueRepository,
    logger: Logger
) : Watcher<JsIssueInfo>(defaultDispatcher, mainDispatcher, repository, logger)
