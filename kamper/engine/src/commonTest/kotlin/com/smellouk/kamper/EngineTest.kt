package com.smellouk.kamper

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.Watcher
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class EngineTest {
    private val performance = mockk<Performance<Config, Watcher<Info>, Info>>(relaxed = true)

    private val classToTest = spyk<Engine>()

    @BeforeTest
    fun setup() {
        classToTest.performanceList.clear()
        classToTest.mapListeners.clear()
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

        assertEquals(0, classToTest.mapListeners.size)
    }

    @Test
    fun `addInfoListener should add new listener to the current map listeners when performance module is installed`() {
        classToTest.install(createPerformanceModule(true))

        classToTest.addInfoListener<Info> { }

        assertEquals(1, classToTest.mapListeners.size)
    }

    @Test
    fun `removeInfoListener should add remove the added listener from the current map listeners`() {
        classToTest.install(createPerformanceModule(true))
        val listener = mockk<InfoListener<Info>>(relaxed = true)
        classToTest.addInfoListener(listener)

        classToTest.removeInfoListener<Info>()

        assertEquals(0, classToTest.mapListeners.size)
    }

    @Test
    fun `install should initialize performance and add it to list of performances`() {
        val performanceMock = createPerformance(true)
        val performanceModule = createPerformanceModule(true, performanceMock)

        classToTest.install(performanceModule)

        assertEquals(1, classToTest.mapListeners.size)
        assertEquals(1, classToTest.performanceList.size)
        verify { performanceMock.initialize(any(), any()) }
    }

    @Test
    fun `install should init performance and should not add it to list when module fail to init`() {
        val performanceMock = createPerformance(false)
        val performanceModule = createPerformanceModule(true, performanceMock)

        classToTest.install(performanceModule)

        assertEquals(1, classToTest.mapListeners.size)
        assertEquals(0, classToTest.performanceList.size)
        verify { performanceMock.initialize(any(), any()) }
    }

    @Test
    fun `install should init performance and should not add it to list when config is disabled`() {
        val performanceMock = createPerformance(true)
        val performanceModule = createPerformanceModule(false, performanceMock)

        classToTest.install(performanceModule)

        assertEquals(1, classToTest.mapListeners.size)
        assertEquals(0, classToTest.performanceList.size)
        verify(exactly = 0) { performanceMock.initialize(any(), any()) }
    }

    private fun createPerformance(isInitialized: Boolean): Performance<Config, Watcher<Info>, Info> =
        mockk<Performance<Config, Watcher<Info>, Info>>().apply {
            every { initialize(any(), any()) } returns isInitialized
        }

    private fun createPerformanceModule(
        isConfigEnabled: Boolean,
        performanceMock: Performance<Config, Watcher<Info>, Info>? = null
    ): PerformanceModule<Config, Info> {
        return mockk<PerformanceModule<Config, Info>>(relaxed = true).apply {
            every { config.isEnabled } returns isConfigEnabled
            if (performanceMock != null) {
                every { performance } returns performanceMock
            }
        }
    }
}
