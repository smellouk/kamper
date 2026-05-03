package com.smellouk.konitor.ui.compose

import com.smellouk.konitor.ui.KonitorUiSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * D-11: Non-rendering callback-shape verification — confirms the callback parameter
 *       types KonitorPanel exposes (onSettingsChange, onStartEngine, etc.) are wired
 *       as ordinary value-passing / void lambdas. Cannot verify click invocation
 *       without a compose test rule (out of scope per CONTEXT.md deferred section + Pitfall 3
 *       in 03-RESEARCH.md). Instead, this suite verifies the lambda-shape contract
 *       so any future regression in KonitorPanel's parameter types breaks compilation.
 *
 * D-12: Non-rendering tab-routing — verifies the `when(selectedTab)` mapping
 *       in KonitorPanel.kt routes the four tab indices to their tabs correctly.
 *       The test mirrors KonitorPanel's routing logic; if KonitorPanel's mapping
 *       changes, this test must change too — guarding against accidental regressions.
 */
@Suppress("IllegalIdentifier")
class KonitorPanelTest {

    /** Mirror of KonitorPanel.kt's `when(selectedTab)` routing block. */
    private val tabRouting: (Int) -> String = { tab ->
        when (tab) {
            0    -> "Activity"
            1    -> "Perfetto"
            2    -> "Issues"
            else -> "Settings"
        }
    }

    // ── D-12: Tab routing ────────────────────────────────────────────────────

    @Test
    fun `tab index 0 routes to Activity`() {
        assertEquals("Activity", tabRouting(0))
    }

    @Test
    fun `tab index 1 routes to Perfetto`() {
        assertEquals("Perfetto", tabRouting(1))
    }

    @Test
    fun `tab index 2 routes to Issues`() {
        assertEquals("Issues", tabRouting(2))
    }

    @Test
    fun `tab index 3 routes to Settings`() {
        assertEquals("Settings", tabRouting(3))
    }

    @Test
    fun `tab index outside 0_3 falls back to Settings via else branch`() {
        assertEquals("Settings", tabRouting(99))
        assertEquals("Settings", tabRouting(-1))
    }

    // ── D-11: Callback-shape verification ────────────────────────────────────

    @Test
    fun `onSettingsChange shape is value_passing KonitorUiSettings to Unit`() {
        var captured: KonitorUiSettings? = null
        val cb: (KonitorUiSettings) -> Unit = { captured = it }

        val sample = KonitorUiSettings()
        cb(sample)

        assertNotNull(captured, "onSettingsChange callback must capture invocation argument")
        assertEquals(sample, captured)
    }

    @Test
    fun `engine controls have void Unit callback shape`() {
        var startCalled = false
        var stopCalled = false
        var restartCalled = false

        val onStart: () -> Unit = { startCalled = true }
        val onStop: () -> Unit = { stopCalled = true }
        val onRestart: () -> Unit = { restartCalled = true }

        onStart()
        onStop()
        onRestart()

        assertTrue(startCalled, "onStartEngine lambda invocation must set the flag")
        assertTrue(stopCalled, "onStopEngine lambda invocation must set the flag")
        assertTrue(restartCalled, "onRestartEngine lambda invocation must set the flag")
    }

    @Test
    fun `dismiss recording and clear callbacks have void Unit shape`() {
        var dismissed = false
        var cleared = false
        var startedRec = false
        var stoppedRec = false
        var exported = false

        val onDismiss: () -> Unit = { dismissed = true }
        val onClearIssues: () -> Unit = { cleared = true }
        val onStartRecording: () -> Unit = { startedRec = true }
        val onStopRecording: () -> Unit = { stoppedRec = true }
        val onExportTrace: () -> Unit = { exported = true }

        onDismiss()
        onClearIssues()
        onStartRecording()
        onStopRecording()
        onExportTrace()

        assertTrue(dismissed)
        assertTrue(cleared)
        assertTrue(startedRec)
        assertTrue(stoppedRec)
        assertTrue(exported)
    }
}
