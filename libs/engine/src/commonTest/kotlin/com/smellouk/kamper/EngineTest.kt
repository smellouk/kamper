package com.smellouk.kamper

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.IWatcher
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class EngineTest {
    private val performance = mock<Performance<Config, IWatcher<Info>, Info>>(MockMode.autofill)

    private val classToTest = Engine()

    @BeforeTest
    fun setup() {
        classToTest.performanceList.clear()
        classToTest.mapListeners.clear()
        // Mirror Engine.init {} re-seeding so each test starts in the same state Engine guarantees.
        classToTest.mapListeners[ValidationInfo::class] = mutableListOf()
    }

    @Test
    fun `start should start all performance list`() {
        classToTest.performanceList.add(performance)

        classToTest.start()

        verify { performance.start() }
    }

    @Test
    fun `stop should stop all performance list`() {
        classToTest.performanceList.add(performance)

        classToTest.stop()

        verify { performance.stop() }
    }

    @Test
    fun `clear should clear all performance list`() {
        classToTest.performanceList.add(performance)

        classToTest.clear()

        assertEquals(0, classToTest.performanceList.size)
    }

    @Test
    fun `addInfoListener should not add listener when target performance module is not installed`() {
        classToTest.addInfoListener<Info> { }

        // Engine.init {} seeds the ValidationInfo slot; adding an Info listener for a
        // non-installed module does not add a new slot — size stays at 1 (ValidationInfo only).
        assertEquals(1, classToTest.mapListeners.size)
    }

    @Test
    fun `addInfoListener should add new listener to the current map listeners when performance module is installed`() {
        classToTest.install(createPerformanceModule(true))

        classToTest.addInfoListener<Info> { }

        // ValidationInfo slot (1) + Info slot from install (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)
    }

    @Test
    fun `removeInfoListener should remove the specific listener while keeping the map entry`() {
        classToTest.install(createPerformanceModule(true))
        val listener: InfoListener<Info> = { }
        classToTest.addInfoListener(listener)

        classToTest.removeInfoListener(listener)

        // ValidationInfo slot (1) + Info slot from install (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)
        // The internal integration fan-out listener registered by install() remains; only
        // the user listener was removed. Size is 1 (internal fan-out), not 0.
        assertEquals(1, classToTest.mapListeners[Info::class]?.size)
    }

    @Test
    fun `removeInfoListener should do nothing when listener was not added`() {
        classToTest.install(createPerformanceModule(true))
        val listener: InfoListener<Info> = { }

        classToTest.removeInfoListener(listener)

        // ValidationInfo slot (1) + Info slot from install (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)
        // The internal integration fan-out listener registered by install() is present.
        // No user listener was added, so size is 1 (internal fan-out only).
        assertEquals(1, classToTest.mapListeners[Info::class]?.size)
    }

    @Test
    fun `uninstall should stop and remove the performance and its listeners`() {
        val performanceMock = createPerformance(true)
        val module = createPerformanceModule(true, performanceMock)
        classToTest.install(module)
        assertEquals(1, classToTest.performanceList.size)
        // ValidationInfo slot (1) + Info slot from install (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)

        classToTest.uninstall(module)

        assertEquals(0, classToTest.performanceList.size)
        // After uninstall, Info slot is removed but ValidationInfo slot remains = 1.
        assertEquals(1, classToTest.mapListeners.size)
    }

    @Test
    fun `uninstall should do nothing when module is not installed`() {
        val module = createPerformanceModule(true)

        classToTest.uninstall(module)

        assertEquals(0, classToTest.performanceList.size)
        // Only ValidationInfo slot seeded by Engine.init {} = 1.
        assertEquals(1, classToTest.mapListeners.size)
    }

    @Test
    fun `uninstall should allow reinstall after removal`() {
        val performanceMock = createPerformance(true)
        val module = createPerformanceModule(true, performanceMock)
        classToTest.install(module)
        classToTest.uninstall(module)

        classToTest.install(module)

        assertEquals(1, classToTest.performanceList.size)
        // ValidationInfo slot (1) + Info slot from reinstall (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)
    }

    @Test
    fun `install should initialize performance and add it to list of performances`() {
        val performanceMock = createPerformance(true)
        val performanceModule = createPerformanceModule(true, performanceMock)

        classToTest.install(performanceModule)

        // ValidationInfo slot (1) + Info slot from install (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)
        assertEquals(1, classToTest.performanceList.size)
        verify { performanceMock.initialize(any(), any()) }
    }

    @Test
    fun `install should init performance and should not add it to list when module fail to init`() {
        val performanceMock = createPerformance(false)
        val performanceModule = createPerformanceModule(true, performanceMock)

        classToTest.install(performanceModule)

        // ValidationInfo slot (1) + Info slot from install attempt (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)
        assertEquals(0, classToTest.performanceList.size)
        verify { performanceMock.initialize(any(), any()) }
    }

    @Test
    fun `install should init performance and should not add it to list when config is disabled`() {
        val performanceMock = createPerformance(true)
        val performanceModule = createPerformanceModule(false, performanceMock)

        classToTest.install(performanceModule)

        // ValidationInfo slot (1) + Info slot from install attempt (1) = 2 total.
        assertEquals(2, classToTest.mapListeners.size)
        assertEquals(0, classToTest.performanceList.size)
        verify(exactly(0)) { performanceMock.initialize(any(), any()) }
    }

    private fun createPerformance(isInitialized: Boolean): Performance<Config, IWatcher<Info>, Info> =
        mock<Performance<Config, IWatcher<Info>, Info>>().also {
            every { it.initialize(any(), any()) } returns isInitialized
            every { it.stop() } returns Unit
        }

    private fun createPerformanceModule(
        isConfigEnabled: Boolean,
        performanceMock: Performance<Config, IWatcher<Info>, Info>? = null
    ): PerformanceModule<Config, Info> {
        val config = mock<Config>().also {
            every { it.isEnabled } returns isConfigEnabled
            every { it.intervalInMs } returns 1000L
        }
        val perf = performanceMock ?: mock<Performance<Config, IWatcher<Info>, Info>>(MockMode.autofill)
        return PerformanceModule(config, perf)
    }
}
