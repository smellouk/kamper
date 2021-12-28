package com.smellouk.kamper.api

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class PerformanceTest {
    private val watcher = mockk<Watcher<Info>>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private lateinit var classToTest: Performance<Config, Watcher<Info>, Info>

    @BeforeTest
    fun setup() {
        classToTest = Performance(watcher, logger)
    }

    @Test
    fun `initialize should return false when config_intervalInMs is bellow or equals to 0`() {
        val config = mockConfig(INVALID_INTERVAL_IN_MS)

        val result = classToTest.initialize(config, emptyList())

        assertFalse(result)
    }

    @Test
    fun `initialize should return true when finish initializing`() {
        val config = mockConfig()

        val result = classToTest.initialize(config, emptyList())

        assertTrue(result)
    }

    @Test
    fun `initialize should return true when its already initialized`() {
        val config = mockConfig()

        classToTest.initialize(config, LISTENERS_LIST)
        val result = classToTest.initialize(config, LISTENERS_LIST)

        assertTrue(result)
    }

    @Test
    fun `start should not start watcher when performance is not initialized`() {
        classToTest.start()

        verify(exactly = 0) { watcher.startWatching(any(), any()) }
        verify { logger.log(any()) }
        confirmVerified(watcher, logger)
    }

    @Test
    fun `start should start watcher when performance is initialized`() {
        classToTest.initialize(mockConfig(), LISTENERS_LIST)

        classToTest.start()

        verify(exactly = 1) { watcher.startWatching(INTERVAL_IN_MS, any()) }
        verify(exactly = 0) { logger.log(any()) }
        confirmVerified(watcher, logger)
    }

    @Test
    fun `stop should stop watcher`() {
        classToTest.stop()

        verify(exactly = 1) { watcher.stopWatching() }
        verify(exactly = 0) { watcher.startWatching(any(), any()) }
        verify(exactly = 0) { logger.log(any()) }
        confirmVerified(watcher, logger)
    }

    private fun mockConfig(interval: Long = INTERVAL_IN_MS): Config = mockk<Config>().apply {
        every { intervalInMs } returns interval
    }
}

private const val INTERVAL_IN_MS = 10L
private const val INVALID_INTERVAL_IN_MS = -1L
private val LISTENERS_LIST = emptyList<InfoListener<Info>>()
