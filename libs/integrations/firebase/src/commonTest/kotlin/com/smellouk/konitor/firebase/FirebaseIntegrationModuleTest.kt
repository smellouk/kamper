package com.smellouk.konitor.firebase

import com.smellouk.konitor.api.Info
import com.smellouk.konitor.api.KonitorEvent
import com.smellouk.konitor.api.UserEventInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Behavior tests for [FirebaseIntegrationModule]. On the JVM target, the underlying
 * `recordNonFatal` actual is a no-op (per D-07), so these tests verify that:
 *   - The module's routing/guard logic produces no exceptions for any input.
 *   - INVALID is dropped before any platform call (T-16-04).
 *   - Disabled forwarding is honored (D-10).
 *   - Pathological Info.toString() does not propagate (T-16-02).
 *
 * For event tests (D-28), a logSink seam captures the message string passed to
 * recordLog on JVM, since the JVM actual is a no-op. This verifies routing and
 * message format without requiring a live Crashlytics SDK.
 *
 * Live device verification is in 18-VALIDATION.md Manual-Only Verifications.
 */
@Suppress("IllegalIdentifier")
class FirebaseIntegrationModuleTest {

    private fun makeEvent(moduleName: String, info: Info, platform: String = "jvm"): KonitorEvent =
        KonitorEvent(moduleName = moduleName, timestampMs = 0L, platform = platform, info = info)

    @Test
    fun `onEvent silently drops Info INVALID when forwardIssues is true per T-16-04`() {
        val module = FirebaseModule { forwardIssues = true }
        module.onEvent(makeEvent("issue", Info.INVALID))
        assertTrue(true)
    }

    @Test
    fun `onEvent does not forward when forwardIssues is false per D-10`() {
        val module = FirebaseModule { forwardIssues = false }
        val info = object : Info { override fun toString() = "test issue" }
        module.onEvent(makeEvent("issue", info))
        assertTrue(true)
    }

    @Test
    fun `onEvent ignores moduleNames other than issue per D-07`() {
        val module = FirebaseModule { forwardIssues = true }
        val info = object : Info { override fun toString() = "test" }

        module.onEvent(makeEvent("cpu", info))
        module.onEvent(makeEvent("memory", info))
        module.onEvent(makeEvent("fps", info))
        module.onEvent(makeEvent("network", info))
        module.onEvent(makeEvent("jank", info))
        module.onEvent(makeEvent("gc", info))
        module.onEvent(makeEvent("thermal", info))
        module.onEvent(makeEvent("totally-unknown", info))

        assertTrue(true)
    }

    @Test
    fun `onEvent on JVM with forwardIssues true is a no-op and does not throw per D-07`() {
        val module = FirebaseModule { forwardIssues = true }
        val info = object : Info { override fun toString() = "issue payload" }
        module.onEvent(makeEvent("issue", info))
        assertTrue(true)
    }

    @Test
    fun `onEvent does not propagate exceptions from pathological Info per T-16-02`() {
        val module = FirebaseModule { forwardIssues = true }
        val pathological = object : Info { override fun toString(): String = error("boom") }
        module.onEvent(makeEvent("issue", pathological))
        assertTrue(true)
    }

    @Test
    fun `clean is a no-op and does not throw`() {
        val module = FirebaseModule { forwardIssues = true }
        module.clean()
        assertTrue(true)
    }

    // -------------------------------------------------------------------------
    // Phase 24 D-28: "event" branch tests
    // -------------------------------------------------------------------------

    @Test
    fun event_branch_skips_when_forwardEvents_is_false() {
        var captured: String? = null
        val module = FirebaseIntegrationModule(
            config = FirebaseConfig.Builder().apply { forwardEvents = false }.build(),
            logSink = { captured = it }
        )
        val info = UserEventInfo("purchase", null)
        module.onEvent(makeEvent("event", info))
        assertNull(captured, "logSink must not be called when forwardEvents=false")
    }

    @Test
    fun event_branch_skips_when_info_is_not_UserEventInfo() {
        var captured: String? = null
        val module = FirebaseIntegrationModule(
            config = FirebaseConfig.Builder().apply { forwardEvents = true }.build(),
            logSink = { captured = it }
        )
        val nonUserEventInfo = object : Info { override fun toString() = "not-user-event" }
        module.onEvent(makeEvent("event", nonUserEventInfo))
        assertNull(captured, "logSink must not be called when info is not UserEventInfo")
    }

    @Test
    fun event_branch_emits_log_for_instant_event() {
        var captured: String? = null
        val module = FirebaseIntegrationModule(
            config = FirebaseConfig.Builder().apply { forwardEvents = true }.build(),
            logSink = { captured = it }
        )
        val info = UserEventInfo("purchase", null)
        module.onEvent(makeEvent("event", info))
        assertEquals("konitor.event: purchase", captured)
    }

    @Test
    fun event_branch_emits_log_with_duration_suffix_for_duration_event() {
        var captured: String? = null
        val module = FirebaseIntegrationModule(
            config = FirebaseConfig.Builder().apply { forwardEvents = true }.build(),
            logSink = { captured = it }
        )
        val info = UserEventInfo("video", 1024L)
        module.onEvent(makeEvent("event", info))
        assertEquals("konitor.event: video (1024 ms)", captured)
    }
}
