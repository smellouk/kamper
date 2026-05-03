package com.smellouk.konitor.rn

import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance

internal class JsIssuePerformance(
    watcher: IWatcher<JsIssueInfo>,
    logger: Logger
) : Performance<JsIssueConfig, IWatcher<JsIssueInfo>, JsIssueInfo>(watcher, logger)
