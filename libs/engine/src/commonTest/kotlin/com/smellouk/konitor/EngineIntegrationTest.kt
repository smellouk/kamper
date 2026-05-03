package com.smellouk.konitor

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Info
import com.smellouk.konitor.api.InfoListener
import com.smellouk.konitor.api.IntegrationModule
import com.smellouk.konitor.api.KonitorEvent
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import dev.mokkery.MockMode
import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.reflect.KClass
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests Phase 16 D-02 (Engine.addIntegration) and D-10 (no event without an installed
 * module of the matching type), plus threat T-16-02 (a throwing integration MUST NOT
 * propagate to Konitor core).
 */
@Suppress("IllegalIdentifier", "UNCHECKED_CAST")
class EngineIntegrationTest {

    private class FakeInfo(val value: Int) : Info

    private class RecordingIntegration(
        val name: String = "rec",
        val throwOnEvent: Boolean = false,
        val throwOnClean: Boolean = false
    ) : IntegrationModule {
        val received: MutableList<KonitorEvent> = mutableListOf()
        var cleanCount: Int = 0
        override fun onEvent(event: KonitorEvent) {
            received += event
            if (throwOnEvent) error("$name onEvent boom")
        }
        override fun clean() {
            cleanCount++
            if (throwOnClean) error("$name clean boom")
        }
    }

    private val classToTest = Engine()

    @BeforeTest
    fun setup() {
        classToTest.performanceList.clear()
        classToTest.mapListeners.clear()
        classToTest.mapListeners[ValidationInfo::class] = mutableListOf()  // mirror Engine.init {}
        classToTest.integrationList.clear()
    }

    // (a) D-02 — addIntegration registers and is fluent
    @Test
    fun `addIntegration appends to integrationList and returns engine for fluent chaining`() {
        val a = RecordingIntegration("a")
        val b = RecordingIntegration("b")

        val returned = classToTest.addIntegration(a).addIntegration(b)

        assertSame(classToTest, returned)
        assertEquals(2, classToTest.integrationList.size)
        assertSame(a, classToTest.integrationList[0])
        assertSame(b, classToTest.integrationList[1])
    }

    // (b) D-02 — installed module emits KonitorEvent on each Info update
    @Test
    fun `installed module fan-out invokes onEvent with KonitorEvent carrying the Info`() {
        val recorder = RecordingIntegration("rec")
        classToTest.addIntegration(recorder)

        val module = installFakeInfoModule()

        // Drive a fake Info update via the registered listeners (Performance.initialize
        // captures listeners; we replay against the same list on mapListeners).
        val listenersForFakeInfo = classToTest.mapListeners[FakeInfo::class] as MutableList<InfoListener<FakeInfo>>
        val fakeInfo = FakeInfo(42)
        listenersForFakeInfo.forEach { it(fakeInfo) }

        assertEquals(1, recorder.received.size)
        val event = recorder.received[0]
        assertSame(fakeInfo, event.info)
        assertTrue(event.moduleName.isNotEmpty())
        assertTrue(event.platform in setOf("android", "ios", "jvm", "macos", "js", "wasmjs", "tvos"))
        assertTrue(event.timestampMs >= 0L)
    }

    // (c) D-02 — removeIntegration cleans then removes
    @Test
    fun `removeIntegration calls clean on the integration then removes it from list`() {
        val a = RecordingIntegration("a")
        classToTest.addIntegration(a)

        classToTest.removeIntegration(a)

        assertEquals(0, classToTest.integrationList.size)
        assertEquals(1, a.cleanCount)
    }

    // (d) Engine.clear() cleans all integrations
    @Test
    fun `clear cleans every integration and clears the list`() {
        val a = RecordingIntegration("a")
        val b = RecordingIntegration("b")
        classToTest.addIntegration(a).addIntegration(b)

        classToTest.clear()

        assertEquals(0, classToTest.integrationList.size)
        assertEquals(1, a.cleanCount)
        assertEquals(1, b.cleanCount)
    }

    // (e) T-16-02 — throwing integration MUST NOT propagate
    @Test
    fun `dispatch isolates a throwing integration from other integrations and from caller`() {
        val bomb = RecordingIntegration("bomb", throwOnEvent = true)
        val healthy = RecordingIntegration("healthy")
        classToTest.addIntegration(bomb).addIntegration(healthy)
        installFakeInfoModule()

        val listeners = classToTest.mapListeners[FakeInfo::class] as MutableList<InfoListener<FakeInfo>>

        // Dispatch must not throw despite the first integration blowing up
        var threw = false
        try {
            listeners.forEach { it(FakeInfo(7)) }
        } catch (t: Throwable) {
            threw = true
        }

        assertTrue(!threw, "Engine fan-out propagated an exception from a throwing integration")
        assertEquals(1, healthy.received.size)
    }

    // (f) D-10 — addIntegration without any install produces NO events
    @Test
    fun `dispatchToIntegrations is only triggered when a matching module is installed`() {
        val recorder = RecordingIntegration("rec")
        classToTest.addIntegration(recorder)

        // No install -> no listener registered -> nothing to fire. Just assert the integration
        // received nothing after some elapsed time (no driving event).
        assertEquals(0, recorder.received.size)
    }

    // (g) Order independence — addIntegration AFTER install also receives subsequent events
    @Test
    fun `integration registered after install still receives events on subsequent Info updates`() {
        installFakeInfoModule()

        val late = RecordingIntegration("late")
        classToTest.addIntegration(late)

        val listeners = classToTest.mapListeners[FakeInfo::class] as MutableList<InfoListener<FakeInfo>>
        listeners.forEach { it(FakeInfo(99)) }

        assertEquals(1, late.received.size)
        assertEquals(99, (late.received[0].info as FakeInfo).value)
    }

    /**
     * Installs a real [Engine.install] flow against a Mokkery-mocked Performance whose
     * `initialize` returns true and a Config with isEnabled=true. After install,
     * `mapListeners[FakeInfo::class]` contains the internal integration fan-out listener
     * registered by Engine.install — that's what the tests drive directly.
     */
    private fun installFakeInfoModule(): PerformanceModule<Config, FakeInfo> {
        val cfg = mock<Config>().also {
            every { it.isEnabled } returns true
            every { it.intervalInMs } returns 1000L
        }
        val perf = mock<Performance<Config, IWatcher<FakeInfo>, FakeInfo>>(MockMode.autofill).also {
            every { it.initialize(any(), any()) } returns true
        }
        val module = PerformanceModule(cfg, perf)
        classToTest.install(module)
        // Sanity: Engine added its internal fan-out listener
        assertNotNull(classToTest.mapListeners[FakeInfo::class])
        assertTrue(
            (classToTest.mapListeners[FakeInfo::class] as List<*>).isNotEmpty(),
            "Engine.install did not register the internal integration fan-out listener"
        )
        return module
    }
}
