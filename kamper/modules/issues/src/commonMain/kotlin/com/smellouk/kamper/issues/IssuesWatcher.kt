package com.smellouk.kamper.issues

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.issues.detector.IssueDetector

internal class IssuesWatcher(
    private val detectors: List<IssueDetector>
) : IWatcher<IssueInfo> {
    private var listeners: List<InfoListener<IssueInfo>> = emptyList()

    override fun startWatching(intervalInMs: Long, listeners: List<InfoListener<IssueInfo>>) {
        this.listeners = listeners
        detectors.forEach { detector ->
            detector.start(intervalInMs) { issue ->
                this.listeners.forEach { it.invoke(IssueInfo(issue)) }
            }
        }
    }

    override fun stopWatching() {
        detectors.forEach { it.stop() }
    }

    fun clean() {
        detectors.forEach { it.clean() }
    }
}
