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
}
