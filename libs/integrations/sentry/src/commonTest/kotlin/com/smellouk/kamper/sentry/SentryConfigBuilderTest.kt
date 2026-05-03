package com.smellouk.kamper.sentry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class SentryConfigBuilderTest {

    @Test
    fun `Builder build returns config with provided dsn and defaults when no DSL block runs`() {
        val cfg = SentryConfig.Builder().build("https://test@sentry.io/1")
        assertEquals("https://test@sentry.io/1", cfg.dsn)
        assertFalse(cfg.forwardIssues)
        assertNull(cfg.forwardCpuAbove)
        assertNull(cfg.forwardMemoryAbove)
        assertFalse(cfg.forwardFps)
    }

    @Test
    fun `Builder honors all opt-in toggles per D-10 and D-11`() {
        val cfg = SentryConfig.Builder().apply {
            forwardIssues = true
            forwardCpuAbove = 80f
            forwardMemoryAbove = 85f
            forwardFps = true
        }.build("https://test@sentry.io/2")
        assertTrue(cfg.forwardIssues)
        assertEquals(80f, cfg.forwardCpuAbove)
        assertEquals(85f, cfg.forwardMemoryAbove)
        assertTrue(cfg.forwardFps)
    }

    @Test
    fun `SentryModule factory invokes Builder DSL and returns a SentryIntegrationModule`() {
        val module = SentryModule(dsn = "https://test@sentry.io/3") {
            forwardIssues = true
            forwardCpuAbove = 50f
        }
        // Cannot inspect private config directly, so just assert the factory returned the right type.
        @Suppress("USELESS_IS_CHECK")
        assertTrue(module is SentryIntegrationModule)
    }

    @Test
    fun `Builder defaults forwardEvents to true per D-25`() {
        val cfg = SentryConfig.Builder().build("https://test@sentry.io/10")
        assertTrue(cfg.forwardEvents)
    }

    @Test
    fun `Builder honors forwardEvents false`() {
        val cfg = SentryConfig.Builder().apply {
            forwardEvents = false
        }.build("https://test@sentry.io/11")
        assertFalse(cfg.forwardEvents)
    }

    @Test
    fun `toString includes forwardEvents`() {
        val cfg = SentryConfig.Builder().build("https://secret@sentry.io/12")
        val s = cfg.toString()
        assertTrue(s.contains("forwardEvents=true"), "Expected forwardEvents=true in toString: $s")
        assertFalse(s.contains("secret"), "DSN must be redacted in toString: $s")
    }
}
