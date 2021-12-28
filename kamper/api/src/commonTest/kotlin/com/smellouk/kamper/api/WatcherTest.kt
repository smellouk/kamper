package com.smellouk.kamper.api

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@ExperimentalCoroutinesApi
@Suppress("IllegalIdentifier")
class WatcherTest {
    private val info = mockk<Info>(relaxed = true)
    private val listener: InfoListener<Info> = mockk()

    private val testScope = TestScope()
    private val defaultDispatcher = StandardTestDispatcher(testScope.testScheduler)
    private val mainDispatcher = defaultDispatcher
    private val infoRepository = mockk<InfoRepository<Info>>()
    private val logger = mockk<Logger>(relaxed = true)

    private val classToTest: Watcher<Info> by lazy {
        Watcher(
            defaultDispatcher = defaultDispatcher,
            mainDispatcher = mainDispatcher,
            infoRepository = infoRepository,
            logger = logger
        )
    }

    @Test
    fun `startWatching should run scope loop with 2 runs and should log the gotten info`() =
        runTest {
            every { listener(info) } returns Unit
            every { infoRepository.getInfo() } returns info

            classToTest.startWatching(INTERVAL_IN_MS, listOf(listener)).also {
                loopTwiceAndCancel()
            }

            verify(exactly = 2) { logger.log(info.toString()) }
            verify(exactly = 2) { listener.invoke(info) }
            confirmVerified(logger, listener)
        }

    @Test
    fun `startWatching should run scope loop with 2 runs and should not report info when infoRepository throw `() =
        runTest {
            every { infoRepository.getInfo() } throws mockk<Exception>(relaxed = true)

            classToTest.startWatching(INTERVAL_IN_MS, listOf(listener)).also {
                loopTwiceAndCancel()
            }

            verify(exactly = 2) { logger.log(any()) }
            verify(exactly = 0) { listener.invoke(info) }
            confirmVerified(logger, listener)
        }

    @Test
    fun `stopWatching should cancel current job`() {
        val job = mockk<Job>(relaxed = true)
        classToTest.job = job

        classToTest.stopWatching()

        verify { job.cancel() }
        confirmVerified(job)
    }

    private fun loopTwiceAndCancel() {
        // first Run
        testScope.advanceTimeBy(INTERVAL_IN_MS)
        testScope.runCurrent()

        // 2nd Run
        testScope.advanceTimeBy(INTERVAL_IN_MS)
        testScope.runCurrent()

        // Job finished
        defaultDispatcher.cancel()
    }
}

private const val INTERVAL_IN_MS = 2000L
