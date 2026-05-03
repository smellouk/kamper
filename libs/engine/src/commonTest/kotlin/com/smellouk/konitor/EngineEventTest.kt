package com.smellouk.konitor

import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.api.IntegrationModule
import com.smellouk.konitor.api.KonitorEvent
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.UserEventInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 24 D-01..D-12 verification.
 * Plan 04 — Engine event API + buffer + KonitorConfig.eventsEnabled.
 */
@Suppress("IllegalIdentifier")
class EngineEventTest {

    private class RecordingLogger : Logger {
        val lines: MutableList<String> = mutableListOf()
        override fun log(message: String) { lines += message }
    }

    private class RecordingIntegration : IntegrationModule {
        val events: MutableList<KonitorEvent> = mutableListOf()
        override fun onEvent(event: KonitorEvent) { events += event }
        override fun clean() = Unit
    }

    private fun engine(eventsEnabled: Boolean = true, logger: Logger = Logger.EMPTY): Engine =
        Engine().also { e ->
            e.config = KonitorConfig.Builder().apply {
                this.logger = logger
                this.eventsEnabled = eventsEnabled
            }.build()
        }

    @Test
    fun logEvent_buffers_record_and_dispatches_KonitorEvent() {
        val sink = RecordingIntegration()
        val e = engine().also { it.addIntegration(sink) }
        e.logEvent("checkout")
        val buf = e.drainEvents()
        assertEquals(1, buf.size)
        assertEquals("checkout", buf[0].name)
        assertNull(buf[0].durationNs)
        assertEquals(1, sink.events.size)
        assertEquals("event", sink.events[0].moduleName)
        val info = sink.events[0].info as UserEventInfo
        assertEquals("checkout", info.name)
        assertNull(info.durationMs)
    }

    @Test
    fun startEvent_returns_EventToken_with_name_and_startNs() {
        val e = engine()
        val token = e.startEvent("session")
        assertEquals("session", token.name)
        assertTrue(token.startNs > 0L)
    }

    @Test
    fun endEvent_computes_durationNs_buffers_and_dispatches() {
        val sink = RecordingIntegration()
        val e = engine().also { it.addIntegration(sink) }
        val token = e.startEvent("video")
        // Spin until at least one ns has elapsed on the engine clock.
        val target = token.startNs + 1L
        while (engineCurrentTimeNs() < target) { /* spin */ }
        e.endEvent(token)
        val buf = e.drainEvents()
        assertEquals(1, buf.size)
        val rec = buf[0]
        assertEquals("video", rec.name)
        assertNotNull(rec.durationNs)
        assertTrue(rec.durationNs!! > 0L)
        assertEquals(1, sink.events.size)
        val info = sink.events[0].info as UserEventInfo
        assertNotNull(info.durationMs)
    }

    @Test
    fun measureEvent_runs_block_returns_result_and_calls_endEvent_in_finally() {
        val e = engine()
        val r: Int = e.measureEvent("calc") { 42 }
        assertEquals(42, r)
        assertEquals(1, e.drainEvents().size)
        assertNotNull(e.drainEvents()[0].durationNs)
    }

    @Test
    fun measureEvent_calls_endEvent_even_when_block_throws() {
        val e = engine()
        assertFailsWith<IllegalStateException> {
            e.measureEvent<Unit>("boom") { error("nope") }
        }
        assertEquals(1, e.drainEvents().size)
        assertEquals("boom", e.drainEvents()[0].name)
    }

    @Test
    fun eventsEnabled_false_makes_logEvent_a_noop() {
        val sink = RecordingIntegration()
        val e = engine(eventsEnabled = false).also { it.addIntegration(sink) }
        e.logEvent("ignored")
        assertTrue(e.drainEvents().isEmpty())
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun eventsEnabled_false_makes_startEvent_endEvent_a_noop() {
        val sink = RecordingIntegration()
        val e = engine(eventsEnabled = false).also { it.addIntegration(sink) }
        val token = e.startEvent("ignored")
        assertEquals(0L, token.startNs)
        e.endEvent(token)
        assertTrue(e.drainEvents().isEmpty())
        assertTrue(sink.events.isEmpty())
    }

    @Test
    fun dumpEvents_formats_buffer_via_logger() {
        val log = RecordingLogger()
        val e = engine(logger = log)
        e.logEvent("first")
        val token = e.startEvent("slow")
        val target = token.startNs + 1L
        while (engineCurrentTimeNs() < target) { /* spin */ }
        e.endEvent(token)
        e.dumpEvents()
        val text = log.lines.joinToString("\n")
        assertTrue(text.contains("===> Konitor Events: begin"))
        assertTrue(text.contains("first"))
        assertTrue(text.contains("slow"))
        assertTrue(text.contains("worst duration:"))
        assertTrue(text.contains("total events: 2"))
        assertTrue(text.contains("<=== Konitor Events: end"))
    }

    @Test
    fun dumpEvents_on_empty_buffer_is_graceful() {
        val log = RecordingLogger()
        val e = engine(logger = log)
        e.dumpEvents()
        val text = log.lines.joinToString("\n")
        assertTrue(text.contains("===> Konitor Events: begin"))
        assertTrue(text.contains("total events: 0"))
        assertTrue(text.contains("<=== Konitor Events: end"))
        assertTrue(!text.contains("worst duration:"))
    }

    @Test
    fun eventBuffer_caps_at_1000_evicting_oldest() {
        val e = engine()
        for (i in 0..1000) {
            e.logEvent("e$i")
        }
        val buf = e.drainEvents()
        assertEquals(1000, buf.size)
        assertEquals("e1", buf.first().name)
        assertEquals("e1000", buf.last().name)
    }

    @Test
    fun drainEvents_returns_snapshot_without_clearing() {
        val e = engine()
        e.logEvent("a")
        e.logEvent("b")
        e.logEvent("c")
        val s1 = e.drainEvents()
        val s2 = e.drainEvents()
        assertEquals(3, s1.size)
        assertEquals(3, s2.size)
        assertEquals(s1, s2)
        e.logEvent("d")
        assertEquals(4, e.drainEvents().size)
    }

    @Test
    fun clear_empties_eventBuffer() {
        val e = engine()
        e.logEvent("x")
        e.logEvent("y")
        e.logEvent("z")
        e.clear()
        assertTrue(e.drainEvents().isEmpty())
    }

    // ── UserEventInfo listener tests ──────────────────────────────────────

    @Test
    fun addInfoListener_UserEventInfo_receives_logEvent() {
        val received = mutableListOf<UserEventInfo>()
        val e = engine()
        e.addInfoListener<UserEventInfo> { received += it }
        e.logEvent("purchase")
        assertEquals(1, received.size)
        assertEquals("purchase", received[0].name)
        assertNull(received[0].durationMs)
    }

    @Test
    fun addInfoListener_UserEventInfo_receives_endEvent_with_duration() {
        val received = mutableListOf<UserEventInfo>()
        val e = engine()
        e.addInfoListener<UserEventInfo> { received += it }
        val token = e.startEvent("video")
        val target = token.startNs + 1L
        while (engineCurrentTimeNs() < target) { /* spin */ }
        e.endEvent(token)
        assertEquals(1, received.size)
        assertEquals("video", received[0].name)
        assertNotNull(received[0].durationMs)
        assertTrue(received[0].durationMs!! >= 0L)
    }

    @Test
    fun removeInfoListener_UserEventInfo_stops_receiving() {
        val received = mutableListOf<UserEventInfo>()
        val listener: (UserEventInfo) -> Unit = { received += it }
        val e = engine()
        e.addInfoListener(listener)
        e.logEvent("before")
        e.removeInfoListener(listener)
        e.logEvent("after")
        assertEquals(1, received.size)
        assertEquals("before", received[0].name)
    }

    @Test
    fun addInfoListener_UserEventInfo_works_after_clear() {
        val received = mutableListOf<UserEventInfo>()
        val e = engine()
        e.clear()
        e.addInfoListener<UserEventInfo> { received += it }
        e.logEvent("post_clear")
        assertEquals(1, received.size)
        assertEquals("post_clear", received[0].name)
    }

    @Test
    fun listener_and_integration_both_receive_same_event() {
        val listenerReceived = mutableListOf<UserEventInfo>()
        val integrationReceived = RecordingIntegration()
        val e = engine()
        e.addInfoListener<UserEventInfo> { listenerReceived += it }
        e.addIntegration(integrationReceived)
        e.logEvent("checkout")
        assertEquals(1, listenerReceived.size)
        assertEquals(1, integrationReceived.events.size)
        assertEquals("checkout", listenerReceived[0].name)
        assertEquals("checkout", (integrationReceived.events[0].info as UserEventInfo).name)
    }
}
