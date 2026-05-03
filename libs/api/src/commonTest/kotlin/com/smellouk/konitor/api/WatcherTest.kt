package com.smellouk.konitor.api

import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import dev.mokkery.verifyNoMoreCalls
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("IllegalIdentifier")
class WatcherTest {
    private val info = mock<Info>(MockMode.autofill)
    private val listener = mock<MockableListener<Info>>()
    private var callbackInvocations = 0
    private val callback: () -> Unit = { callbackInvocations++ }

    private val testScope = TestScope()
    private val defaultDispatcher = StandardTestDispatcher(testScope.testScheduler)
    private val mainDispatcher = defaultDispatcher
    private val infoRepository = mock<InfoRepository<Info>>()
    private val logger = mock<Logger>(MockMode.autofill)

    @BeforeTest
    fun resetCounter() {
        callbackInvocations = 0
    }

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
            every { listener.invoke(info) } returns Unit
            every { infoRepository.getInfo() } returns info
            val infoString = info.toString()

            classToTest.startWatching(INTERVAL_IN_MS, listOf(listener)).also {
                loopTwiceAndCancel()
            }

            verify(exactly(2)) { logger.log(infoString) }
            verify(exactly(2)) { listener.invoke(info) }
            verifyNoMoreCalls(logger, listener)
        }

    @Test
    fun `startWatching should run scope loop with 2 runs and should not report info when infoRepository throw `() =
        runTest {
            every { infoRepository.getInfo() } throws Exception("test")

            classToTest.startWatching(INTERVAL_IN_MS, listOf(listener)).also {
                loopTwiceAndCancel()
            }

            verify(exactly(2)) { logger.log(any()) }
            verify(exactly(0)) { listener.invoke(any()) }
            verifyNoMoreCalls(logger, listener)
        }

    @Test
    fun `stopWatching should cancel current job`() {
        val job = mock<Job>(MockMode.autofill)
        classToTest.job = job

        classToTest.stopWatching()

        verify { job.cancel() }
        verifyNoMoreCalls(job)
    }

    @Test
    fun `startWatching should invoke onSampleDelivered once per non-null info delivery`() = runTest {
        every { listener.invoke(info) } returns Unit
        every { infoRepository.getInfo() } returns info

        classToTest.startWatching(
            intervalInMs = INTERVAL_IN_MS,
            listeners = listOf(listener),
            onSampleDelivered = callback
        ).also { loopTwiceAndCancel() }

        verify(exactly(2)) { listener.invoke(info) }
        assertEquals(2, callbackInvocations)
    }

    @Test
    fun `startWatching should NOT invoke onSampleDelivered when infoRepository throws`() = runTest {
        every { infoRepository.getInfo() } throws Exception("test")

        classToTest.startWatching(
            intervalInMs = INTERVAL_IN_MS,
            listeners = listOf(listener),
            onSampleDelivered = callback
        ).also { loopTwiceAndCancel() }

        verify(exactly(0)) { listener.invoke(any()) }
        assertEquals(0, callbackInvocations)
    }

    private fun loopTwiceAndCancel() {
        testScope.advanceTimeBy(INTERVAL_IN_MS)
        testScope.runCurrent()

        testScope.advanceTimeBy(INTERVAL_IN_MS)
        testScope.runCurrent()

        defaultDispatcher.cancel()
    }
}

private const val INTERVAL_IN_MS = 2000L
