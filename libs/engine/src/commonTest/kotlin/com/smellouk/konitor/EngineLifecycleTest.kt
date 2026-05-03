package com.smellouk.konitor

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.Info
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class EngineLifecycleTest {

    private val classToTest = Engine()

    @BeforeTest
    fun setup() {
        classToTest.performanceList.clear()
        classToTest.mapListeners.clear()
    }

    @Test
    fun `full lifecycle Install Enable Disable Uninstall should complete without error`() {
        val performanceMock = createPerformance(isInitialized = true)
        val module = createPerformanceModule(isConfigEnabled = true, performanceMock = performanceMock)

        classToTest.install(module)
        classToTest.start()
        classToTest.stop()
        classToTest.uninstall(module)

        assertEquals(0, classToTest.performanceList.size)
        assertEquals(0, classToTest.mapListeners.size)
    }

    @Test
    fun `rapid cycling 50 times should leave no dangling state`() {
        repeat(50) {
            // Fresh mock per iteration — each call to mock<Performance<...>>() yields a
            // distinct anonymous-subclass ::class identity, so Engine.install's duplicate-type
            // guard `performanceList.any { it::class == performance::class }` does not
            // short-circuit. WITHOUT this fresh-mock-per-iteration design, iterations 2..50
            // would silently no-op and the test would degenerate. Do NOT hoist these two
            // factory calls outside the loop.
            val performanceMock = createPerformance(isInitialized = true)
            val module = createPerformanceModule(isConfigEnabled = true, performanceMock = performanceMock)

            classToTest.install(module)
            classToTest.start()
            classToTest.stop()
            classToTest.uninstall(module)
        }

        assertEquals(0, classToTest.performanceList.size)
        assertEquals(0, classToTest.mapListeners.size)
    }

    private fun createPerformance(isInitialized: Boolean): Performance<Config, IWatcher<Info>, Info> =
        mock<Performance<Config, IWatcher<Info>, Info>>().also {
            every { it.initialize(any(), any()) } returns isInitialized
            every { it.start() } returns Unit
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
