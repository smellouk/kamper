package com.smellouk.konitor.rn

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Watcher
import kotlinx.coroutines.CoroutineDispatcher

internal class JsIssueWatcher(
    defaultDispatcher: CoroutineDispatcher,
    mainDispatcher: CoroutineDispatcher,
    repository: JsIssueRepository,
    logger: Logger
) : Watcher<JsIssueInfo>(defaultDispatcher, mainDispatcher, repository, logger)
