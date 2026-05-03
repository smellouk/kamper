package com.smellouk.konitor.issues

import com.smellouk.konitor.api.Cleanable
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance

internal class IssuesPerformance(
    private val issuesWatcher: IssuesWatcher,
    logger: Logger
) : Performance<IssuesConfig, IWatcher<IssueInfo>, IssueInfo>(issuesWatcher, logger), Cleanable {
    override fun clean() {
        issuesWatcher.clean()
    }
}
