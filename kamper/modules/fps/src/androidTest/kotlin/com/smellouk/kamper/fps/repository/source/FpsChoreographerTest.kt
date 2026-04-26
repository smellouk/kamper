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

    @Test
    fun `doFrame should not re-register when fpsActive is false`() {
        classToTest.start() // fpsActive = true, posts callback once
        classToTest.stop() // fpsActive = false, removes callback

        classToTest.frameCallback.doFrame(FRAME_NANO_TIME)

        // postFrameCallback was called exactly once by start(), never by doFrame after stop
        verify(exactly = 1) { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun `doFrame should survive listener exception and re-register`() {
        classToTest.start()
        val throwingListener = mockk<FpsChoreographerFrameListener>()
        every { throwingListener.invoke(any()) } throws RuntimeException("boom")
        classToTest.setFrameListener(throwingListener)

        // Must not throw out of doFrame
        classToTest.frameCallback.doFrame(FRAME_NANO_TIME)

        // Re-registration must still happen because fpsActive is still true
        verify(exactly = 2) { choreographer.postFrameCallback(any()) }
    }
}

private const val FRAME_NANO_TIME = 1000000000L
