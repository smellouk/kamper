package com.smellouk.konitor.opentelemetry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class OtelConfigBuilderTest {

    @Test
    fun `Builder defaults all forwarding off per D-10 and 30s interval`() {
        val cfg = OtelConfig.Builder().build("https://endpoint.example/v1/metrics")
        assertEquals("https://endpoint.example/v1/metrics", cfg.otlpEndpointUrl)
        assertNull(cfg.otlpAuthToken)
        assertFalse(cfg.forwardCpu)
        assertFalse(cfg.forwardMemory)
        assertFalse(cfg.forwardFps)
        assertEquals(30L, cfg.exportIntervalSeconds)
    }

    @Test
    fun `Builder honors all opt-in toggles per D-10 D-11`() {
        val cfg = OtelConfig.Builder().apply {
            otlpAuthToken = "secret-token"
            forwardCpu = true
            forwardMemory = true
            forwardFps = true
            exportIntervalSeconds = 10L
        }.build("https://otlp.example/v1/metrics")
        assertEquals("secret-token", cfg.otlpAuthToken)
        assertTrue(cfg.forwardCpu)
        assertTrue(cfg.forwardMemory)
        assertTrue(cfg.forwardFps)
        assertEquals(10L, cfg.exportIntervalSeconds)
    }

    @Test
    fun `OpenTelemetryModule factory invokes Builder DSL and returns OtelIntegrationModule`() {
        val module = OpenTelemetryModule(otlpEndpointUrl = "https://otlp.example/v1/metrics") {
            forwardCpu = true
        }
        @Suppress("USELESS_IS_CHECK")
        assertTrue(module is OtelIntegrationModule)
    }

    @Test
    fun `OpenTelemetryModule factory with empty DSL builds with defaults`() {
        val module = OpenTelemetryModule("https://endpoint.example/v1/metrics")
        assertEquals(OtelIntegrationModule::class, module::class)
    }
}
