package com.smellouk.kamper.rn

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance

internal class JsIssuePerformance(
    watcher: IWatcher<JsIssueInfo>,
    logger: Logger
) : Performance<JsIssueConfig, IWatcher<JsIssueInfo>, JsIssueInfo>(watcher, logger)
