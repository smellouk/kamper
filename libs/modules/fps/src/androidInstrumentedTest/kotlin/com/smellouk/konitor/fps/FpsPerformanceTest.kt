package com.smellouk.konitor.fps

import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.Watcher
import com.smellouk.konitor.fps.repository.source.FpsChoreographer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class FpsPerformanceTest {
    private val classToTest = spyk(
        FpsPerformance(
            mockk(relaxed = true), mockk(relaxed = true)
        )
    )
    private val parentClassToTest =
        classToTest as Performance<FpsConfig, Watcher<FpsInfo>, FpsInfo>

    @Before
    fun setup() {
        mockkObject(FpsChoreographer)
        every { FpsChoreographer.start() } returns Unit
        every { FpsChoreographer.stop() } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(FpsChoreographer)
    }

    @Test
    fun start_should_call_parent_start_and_FpsChoreographer_start() {
        classToTest.start()

        verify { parentClassToTest.start() }
        verify { FpsChoreographer.start() }
    }

    @Test
    fun stop_should_call_parent_start_and_FpsChoreographer_stop() {
        classToTest.stop()

        verify { parentClassToTest.stop() }
        verify { FpsChoreographer.stop() }
    }

    @Test
    fun clean_should_clean_FpsChoreographer() {
        classToTest.clean()

        verify { FpsChoreographer.clean() }
    }
}
