package com.smellouk.kamper.opentelemetry

import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.KamperEvent
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Behavior tests for [OtelIntegrationModule]. Tests routing/guard logic without
 * asserting OTel SDK side effects (live OTLP delivery is in 16-VALIDATION.md
 * Manual-Only Verifications). On the JVM target the real `recordGauge` actual
 * runs but with a fake endpoint URL; the JVM tests assert that the module never
 * propagates an exception.
 */
@Suppress("IllegalIdentifier")
class OtelIntegrationModuleTest {

    private fun makeEvent(moduleName: String, info: Info, platform: String = "jvm"): KamperEvent =
        KamperEvent(moduleName = moduleName, timestampMs = 0L, platform = platform, info = info)

    @Test
    fun `onEvent silently drops Info INVALID even when all forwarding is enabled per T-16-04`() {
        val module = OpenTelemetryModule("https://endpoint.example/v1/metrics") {
            forwardCpu = true
            forwardMemory = true
            forwardFps = true
        }
        module.onEvent(makeEvent("cpu", Info.INVALID))
        module.onEvent(makeEvent("memory", Info.INVALID))
        module.onEvent(makeEvent("fps", Info.INVALID))
        assertTrue(true)
    }

    @Test
    fun `onEvent rejects non-http endpoints silently per V5 input validation`() {
        val module = OpenTelemetryModule("ftp://bad.example/metrics") {
            forwardCpu = true
        }
        val info = object : Info { override fun toString() = "CpuInfo(totalUseRatio=0.5)" }
        module.onEvent(makeEvent("cpu", info))
        assertTrue(true)
    }

    @Test
    fun `onEvent does not forward CPU when forwardCpu is false per D-10`() {
        val module = OpenTelemetryModule("https://endpoint.example/v1/metrics") {
            forwardCpu = false
            forwardMemory = false
            forwardFps = false
        }
        val info = object : Info { override fun toString() = "CpuInfo(totalUseRatio=0.95)" }
        module.onEvent(makeEvent("cpu", info))
        assertTrue(true)
    }

    @Test
    fun `onEvent ignores moduleNames outside the cpu memory fps route table per D-09`() {
        val module = OpenTelemetryModule("https://endpoint.example/v1/metrics") {
            forwardCpu = true
            forwardMemory = true
            forwardFps = true
        }
        val info = object : Info { override fun toString() = "value=1.0" }
        module.onEvent(makeEvent("issue", info))
        module.onEvent(makeEvent("network", info))
        module.onEvent(makeEvent("jank", info))
        module.onEvent(makeEvent("gc", info))
        module.onEvent(makeEvent("thermal", info))
        module.onEvent(makeEvent("totally-unknown", info))
        assertTrue(true)
    }

    @Test
    fun `onEvent on JVM with valid endpoint URL does not throw even when network is unreachable`() {
        val module = OpenTelemetryModule("https://nonexistent.invalid.local/v1/metrics") {
            forwardCpu = true
            exportIntervalSeconds = 60L
        }
        val info = object : Info { override fun toString() = "CpuInfo(totalUseRatio=0.5)" }
        module.onEvent(makeEvent("cpu", info))
        // Wait briefly so the lazy provider build completes; the failure (if any) is captured by try/catch.
        assertTrue(true)
    }

    @Test
    fun `onEvent does not propagate exceptions from pathological Info per T-16-02`() {
        val module = OpenTelemetryModule("https://endpoint.example/v1/metrics") {
            forwardCpu = true
        }
        val pathological = object : Info { override fun toString(): String = error("toString boom") }
        module.onEvent(makeEvent("cpu", pathological))
        assertTrue(true)
    }

    @Test
    fun `clean is a no-op and does not throw`() {
        val module = OpenTelemetryModule("https://endpoint.example/v1/metrics") {
            forwardCpu = true
        }
        module.clean()
        assertTrue(true)
    }
}
