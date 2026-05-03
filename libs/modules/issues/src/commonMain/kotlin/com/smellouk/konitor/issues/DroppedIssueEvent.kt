package com.smellouk.konitor.issues

/**
 * Emitted by [IssuesWatcher] when the internal issues buffer is at capacity
 * ([IssuesConfig.maxStoredIssues]) and a new issue forces eviction of the
 * oldest entry. Delivered to [IssuesConfig.onDroppedIssue] if non-null.
 *
 * @property droppedIssue The oldest [Issue] that was just evicted from the buffer.
 * @property totalDropped Total count of issues dropped during the current
 *   `startWatching` session. Resets to 0 on each new `startWatching` call
 *   and on `clean()`.
 */
data class DroppedIssueEvent(
    val droppedIssue: Issue,
    val totalDropped: Int
)
