package com.smellouk.kamper.opentelemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD RED: Tests for D-29 — OtelConfig.forwardEvents field.
 * These tests fail before implementation and pass after.
 */
@Suppress("IllegalIdentifier")
class OtelConfigForwardEventsTest {

    @Test
    fun `forwardEvents defaults to true per D-29`() {
        val cfg = OtelConfig.Builder().build("https://endpoint.example/v1/metrics")
        assertTrue(cfg.forwardEvents, "forwardEvents must default to true (D-29)")
    }

    @Test
    fun `DEFAULT_FORWARD_EVENTS constant is true`() {
        assertEquals(true, OtelConfig.DEFAULT_FORWARD_EVENTS)
    }

    @Test
    fun `Builder forwardEvents can be set to false`() {
        val cfg = OtelConfig.Builder().apply {
            forwardEvents = false
        }.build("https://endpoint.example/v1/metrics")
        assertTrue(!cfg.forwardEvents)
    }

    @Test
    fun `toString includes forwardEvents and preserves authToken redaction`() {
        val cfg = OtelConfig.Builder().apply {
            otlpAuthToken = "Bearer secret-token"
            forwardEvents = true
        }.build("https://endpoint.example/v1/metrics")
        val str = cfg.toString()
        assertTrue(str.contains("forwardEvents=true"), "toString must include forwardEvents")
        assertTrue(str.contains("<redacted>"), "toString must redact authToken")
        assertTrue(!str.contains("secret-token"), "authToken must not appear in toString")
    }
}
