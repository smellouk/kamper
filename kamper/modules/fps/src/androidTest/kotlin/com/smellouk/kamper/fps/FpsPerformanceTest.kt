package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.fps.repository.source.FpsChoreographer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

@Suppress("IllegalIdentifier")
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
    fun `start should call parent start and FpsChoreographer start`() {
        classToTest.start()

        verify { parentClassToTest.start() }
        verify { FpsChoreographer.start() }
    }

    @Test
    fun `stop should call parent start and FpsChoreographer stop`() {
        classToTest.stop()

        verify { parentClassToTest.stop() }
        verify { FpsChoreographer.stop() }
    }

    @Test
    fun `clean should clean FpsChoreographer`() {
        classToTest.clean()

        verify { FpsChoreographer.clean() }
    }
}
