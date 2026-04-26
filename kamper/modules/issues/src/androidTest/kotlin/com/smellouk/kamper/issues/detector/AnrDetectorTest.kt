package com.smellouk.kamper.issues.detector

import com.smellouk.kamper.issues.AnrConfig
import com.smellouk.kamper.issues.Issue
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("IllegalIdentifier")
class AnrDetectorTest {
    private val config = AnrConfig(thresholdMs = THRESHOLD_MS)
    private val onIssue = mockk<(Issue) -> Unit>(relaxed = true)

    private fun stoppedField(detector: AnrDetector): Boolean {
        val field = AnrDetector::class.java.getDeclaredField("stopped")
        field.isAccessible = true
        return field.getBoolean(detector)
    }

    @Test
    fun `stop should be safe to call without start`() {
        val detector = AnrDetector(config)
        // Must not throw or deadlock. Even though no thread was started, stop() should be a no-op.
        detector.stop()
    }

    @Test
    fun `stop should return before onIssue can fire after stop`() {
        val detector = AnrDetector(config)
        detector.start(pingIntervalMs = PING_INTERVAL_MS, onIssue = onIssue)

        // Let the watchdog run at least one full cycle.
        Thread.sleep(THRESHOLD_MS * 3)

        // stop() MUST block until watchdog thread has exited (bounded join).
        val before = System.currentTimeMillis()
        detector.stop()
        val elapsed = System.currentTimeMillis() - before

        // Sanity: stop() returned within the bounded window.
        assertTrue(
            "stop() must return within bounded window, elapsed=$elapsed",
            elapsed <= THRESHOLD_MS + JOIN_BUDGET_MS + SAFETY_MARGIN_MS
        )

        // After stop returns, sleep longer than thresholdMs and assert no onIssue fires.
        Thread.sleep(THRESHOLD_MS * 2)
        // verify(exactly = 0) — onIssue never fires AFTER stop() returned
        // (any onIssue calls before stop() are also asserted as 0 because the test runner main thread is responsive)
        verify(exactly = 0) { onIssue.invoke(any()) }
    }

    @Test
    fun `stop should set stopped flag so post-stop guard works`() {
        val detector = AnrDetector(config)
        detector.start(pingIntervalMs = PING_INTERVAL_MS, onIssue = onIssue)
        detector.stop()

        assertTrue("stopped flag must be true after stop()", stoppedField(detector))
    }

    @Test
    fun `start after stop should reset stopped flag for restart`() {
        val detector = AnrDetector(config)
        detector.start(pingIntervalMs = PING_INTERVAL_MS, onIssue = onIssue)
        detector.stop()
        assertTrue("stopped must be true after first stop", stoppedField(detector))

        detector.start(pingIntervalMs = PING_INTERVAL_MS, onIssue = onIssue)
        assertFalse("stopped flag must be reset to false on restart", stoppedField(detector))

        detector.stop() // cleanup
    }
}

private const val THRESHOLD_MS = 100L
private const val PING_INTERVAL_MS = 100L
private const val JOIN_BUDGET_MS = 500L
private const val SAFETY_MARGIN_MS = 500L
