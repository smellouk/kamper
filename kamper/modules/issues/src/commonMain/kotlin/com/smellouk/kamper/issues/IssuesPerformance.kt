package com.smellouk.kamper.issues

import com.smellouk.kamper.api.Cleanable
import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance

internal class IssuesPerformance(
    private val issuesWatcher: IssuesWatcher,
    logger: Logger
) : Performance<IssuesConfig, IWatcher<IssueInfo>, IssueInfo>(issuesWatcher, logger), Cleanable {
    override fun clean() {
        issuesWatcher.clean()
    }
}
