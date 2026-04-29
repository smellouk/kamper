package com.smellouk.kamper.issues

import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.issues.detector.IssueDetector
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class IssuesWatcherTest {

    private val fakeDetector = FakeIssueDetector()
    private val droppedEvents = mutableListOf<DroppedIssueEvent>()
    private val deliveredIssues = mutableListOf<Issue>()
    private val listener: InfoListener<IssueInfo> = { info -> deliveredIssues.add(info.issue) }

    @BeforeTest
    fun setUp() {
        droppedEvents.clear()
        deliveredIssues.clear()
        fakeDetector.reset()
    }

    @Test
    fun `accumulator never exceeds maxStoredIssues across overflow`() {
        val config = IssuesConfig(
            maxStoredIssues = 3,
            onDroppedIssue = { droppedEvents.add(it) }
        )
        val classToTest = IssuesWatcher(detectors = listOf(fakeDetector), config = config)
        classToTest.startWatching(intervalInMs = INTERVAL_IN_MS, listeners = listOf(listener))

        repeat(5) { i -> fakeDetector.fire(issue("id-$i")) }

        // Two overflows: 5 issues - cap of 3 = 2 drops.
        assertEquals(2, droppedEvents.size)
        assertEquals(5, deliveredIssues.size)
    }

    @Test
    fun `dropped events are FIFO and totalDropped is monotonic`() {
        val config = IssuesConfig(
            maxStoredIssues = 2,
            onDroppedIssue = { droppedEvents.add(it) }
        )
        val classToTest = IssuesWatcher(detectors = listOf(fakeDetector), config = config)
        classToTest.startWatching(INTERVAL_IN_MS, listOf(listener))

        fakeDetector.fire(issue("a"))
        fakeDetector.fire(issue("b"))
        fakeDetector.fire(issue("c")) // drops "a" — totalDropped == 1
        fakeDetector.fire(issue("d")) // drops "b" — totalDropped == 2

        assertEquals(2, droppedEvents.size)
        assertEquals("a", droppedEvents[0].droppedIssue.id)
        assertEquals(1, droppedEvents[0].totalDropped)
        assertEquals("b", droppedEvents[1].droppedIssue.id)
        assertEquals(2, droppedEvents[1].totalDropped)
    }

    @Test
    fun `startWatching resets totalDropped between sessions`() {
        val config = IssuesConfig(
            maxStoredIssues = 1,
            onDroppedIssue = { droppedEvents.add(it) }
        )
        val classToTest = IssuesWatcher(detectors = listOf(fakeDetector), config = config)

        classToTest.startWatching(INTERVAL_IN_MS, listOf(listener))
        fakeDetector.fire(issue("x"))
        fakeDetector.fire(issue("y")) // drops "x", totalDropped == 1
        assertEquals(1, droppedEvents.last().totalDropped)

        classToTest.startWatching(INTERVAL_IN_MS, listOf(listener)) // reset
        fakeDetector.fire(issue("p"))
        fakeDetector.fire(issue("q")) // drops "p", totalDropped should be 1, NOT 2
        assertEquals(1, droppedEvents.last().totalDropped)
        assertEquals("p", droppedEvents.last().droppedIssue.id)
    }

    @Test
    fun `clean resets totalDropped and accumulator`() {
        val config = IssuesConfig(
            maxStoredIssues = 1,
            onDroppedIssue = { droppedEvents.add(it) }
        )
        val classToTest = IssuesWatcher(detectors = listOf(fakeDetector), config = config)
        classToTest.startWatching(INTERVAL_IN_MS, listOf(listener))
        fakeDetector.fire(issue("a"))
        fakeDetector.fire(issue("b")) // drops "a" — totalDropped == 1

        classToTest.clean()

        assertTrue(fakeDetector.cleanCalled, "detector.clean() must be propagated")
        // After clean, the next overflow starts a fresh count.
        classToTest.startWatching(INTERVAL_IN_MS, listOf(listener))
        droppedEvents.clear()
        fakeDetector.fire(issue("c"))
        fakeDetector.fire(issue("d")) // drops "c", totalDropped == 1
        assertEquals(1, droppedEvents.single().totalDropped)
    }

    @Test
    fun `null onDroppedIssue does not throw on overflow`() {
        val config = IssuesConfig(maxStoredIssues = 1, onDroppedIssue = null)
        val classToTest = IssuesWatcher(detectors = listOf(fakeDetector), config = config)
        classToTest.startWatching(INTERVAL_IN_MS, listOf(listener))

        fakeDetector.fire(issue("first"))
        fakeDetector.fire(issue("second")) // would NPE if safe-call were missing

        // No callback was registered -> no recorded events. No exception thrown.
        assertEquals(0, droppedEvents.size)
        assertEquals(2, deliveredIssues.size)
    }

    @Test
    fun `listeners receive every issue regardless of drops`() {
        val config = IssuesConfig(
            maxStoredIssues = 2,
            onDroppedIssue = { droppedEvents.add(it) }
        )
        val classToTest = IssuesWatcher(detectors = listOf(fakeDetector), config = config)
        classToTest.startWatching(INTERVAL_IN_MS, listOf(listener))

        repeat(5) { fakeDetector.fire(issue("i-$it")) }

        assertEquals(5, deliveredIssues.size, "all issues delivered to listeners")
        assertEquals(3, droppedEvents.size, "5 issues - cap of 2 = 3 drops")
        assertNull(deliveredIssues.firstOrNull { it.id == "skipped" })
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────

private class FakeIssueDetector : IssueDetector {
    private var onIssue: ((Issue) -> Unit)? = null
    var cleanCalled: Boolean = false

    override fun start(pingIntervalMs: Long, onIssue: (Issue) -> Unit) {
        this.onIssue = onIssue
    }

    override fun stop() {
        this.onIssue = null
    }

    override fun clean() {
        cleanCalled = true
    }

    fun fire(issue: Issue) {
        onIssue?.invoke(issue)
    }

    fun reset() {
        onIssue = null
        cleanCalled = false
    }
}

private fun issue(id: String): Issue = Issue(
    id = id,
    type = IssueType.SLOW_SPAN,
    severity = Severity.INFO,
    message = "test issue $id",
    timestampMs = 0L
)

private const val INTERVAL_IN_MS = 1_000L
