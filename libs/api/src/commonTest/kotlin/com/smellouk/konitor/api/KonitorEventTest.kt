package com.smellouk.konitor.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class KonitorEventTest {

    @Test
    fun `KonitorEvent stores moduleName timestampMs platform and base Info`() {
        val info = object : Info {}
        val event = KonitorEvent(
            moduleName = "cpu",
            timestampMs = 1_700_000_000_000L,
            platform = "jvm",
            info = info
        )

        assertEquals("cpu", event.moduleName)
        assertEquals(1_700_000_000_000L, event.timestampMs)
        assertEquals("jvm", event.platform)
        assertSame(info, event.info)
    }

    @Test
    fun `KonitorEvent supports Info INVALID sentinel as info payload`() {
        val event = KonitorEvent(
            moduleName = "cpu",
            timestampMs = 0L,
            platform = "android",
            info = Info.INVALID
        )

        assertSame(Info.INVALID, event.info)
    }

    @Test
    fun `KonitorEvent equality is value-based for data class semantics`() {
        val info = object : Info {}
        val a = KonitorEvent("cpu", 100L, "jvm", info)
        val b = KonitorEvent("cpu", 100L, "jvm", info)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `IntegrationModule is Cleanable so Engine can call clean on removal`() {
        var cleaned = false
        var receivedEvent: KonitorEvent? = null

        val module: IntegrationModule = object : IntegrationModule {
            override fun onEvent(event: KonitorEvent) {
                receivedEvent = event
            }

            override fun clean() {
                cleaned = true
            }
        }

        // Behaves as a Cleanable
        @Suppress("USELESS_IS_CHECK")
        assertTrue(module is Cleanable)

        // onEvent receives a typed KonitorEvent
        val event = KonitorEvent("cpu", 1L, "jvm", Info.INVALID)
        module.onEvent(event)
        assertSame(event, receivedEvent)

        // clean() is callable
        module.clean()
        assertTrue(cleaned)
    }

    @Test
    fun `currentPlatform returns one of the seven supported platform tags`() {
        val supported = setOf("android", "ios", "jvm", "macos", "js", "wasmjs", "tvos")
        assertTrue(currentPlatform in supported, "currentPlatform was '$currentPlatform'")
    }
}
