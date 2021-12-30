package com.smellouk.kamper.fps.repository.source

import android.view.Choreographer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class FpsChoreographerTest {
    private val choreographer = mockk<Choreographer>(relaxed = true)

    private val classToTest = FpsChoreographer

    @Before
    fun setup() {
        mockkStatic(Choreographer::class)
        every { Choreographer.getInstance() } returns choreographer
        classToTest.clean()
    }

    @Test
    fun `start should get instance of choreographer and postFrameCallback`() {
        classToTest.start()

        verify { Choreographer.getInstance() }
        verify { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun `stop should remove callback from choreographer`() {
        classToTest.start() // To initialize choreographer

        classToTest.stop()

        verify { choreographer.removeFrameCallback(any()) }
    }

    @Test
    fun `stop should not call remove callback from choreographer when no choreographer is present`() {
        classToTest.stop()

        verify(exactly = 0) { choreographer.removeFrameCallback(any()) }
    }

    @Test
    fun `frameCallback should call frameListener`() {
        classToTest.start()
        val frameListener = mockk<FpsChoreographerFrameListener>(relaxed = true)
        classToTest.setFrameListener(frameListener)
        val frameCallback = classToTest.frameCallback

        frameCallback.doFrame(FRAME_NANO_TIME)

        verify { frameListener.invoke(FRAME_NANO_TIME) }
        verify(exactly = 2) { choreographer.postFrameCallback(any()) }
    }
}

private const val FRAME_NANO_TIME = 1000000000L
