package com.smellouk.kamper.issues

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.issues.detector.IssueDetector

internal class IssuesWatcher(
    private val detectors: List<IssueDetector>,
    private val config: IssuesConfig
) : IWatcher<IssueInfo> {
    private var listeners: List<InfoListener<IssueInfo>> = emptyList()

    // Capped FIFO accumulator + drop counter. AnrDetector fires onIssue() from a
    // background thread on JVM/Android and from Dispatchers.Default on Native, so
    // all compound reads/writes on the accumulator are guarded by `lock`.
    private val lock = IssuesLock()
    private val accumulator = ArrayDeque<Issue>()
    private var totalDropped = 0

    override fun startWatching(
        intervalInMs: Long,
        listeners: List<InfoListener<IssueInfo>>,
        onSampleDelivered: (() -> Unit)?
    ) {
        this.listeners = listeners
        lock.withLock {
            accumulator.clear()
            totalDropped = 0
        }
        detectors.forEach { detector ->
            detector.start(intervalInMs) { issue ->
                val droppedEvent: DroppedIssueEvent? = lock.withLock {
                    if (accumulator.size >= config.maxStoredIssues) {
                        val dropped = accumulator.removeFirst()
                        totalDropped += 1
                        DroppedIssueEvent(dropped, totalDropped).also { accumulator.addLast(issue) }
                    } else {
                        accumulator.addLast(issue)
                        null
                    }
                }
                droppedEvent?.let { config.onDroppedIssue?.invoke(it) }
                this.listeners.forEach { it.invoke(IssueInfo(issue)) }
            }
        }
    }

    override fun stopWatching() {
        detectors.forEach { it.stop() }
    }

    fun clean() {
        detectors.forEach { it.clean() }
        lock.withLock {
            accumulator.clear()
            totalDropped = 0
        }
    }
}
