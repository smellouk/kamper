package com.smellouk.konitor.fps.repository.source

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
    fun start_should_get_instance_of_choreographer_and_postFrameCallback() {
        classToTest.start()

        verify { Choreographer.getInstance() }
        verify { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun stop_should_remove_callback_from_choreographer() {
        classToTest.start() // To initialize choreographer

        classToTest.stop()

        verify { choreographer.removeFrameCallback(any()) }
    }

    @Test
    fun stop_should_not_call_remove_callback_from_choreographer_when_no_choreographer_is_present() {
        classToTest.stop()

        verify(exactly = 0) { choreographer.removeFrameCallback(any()) }
    }

    @Test
    fun frameCallback_should_call_frameListener() {
        classToTest.start()
        val frameListener = mockk<FpsChoreographerFrameListener>(relaxed = true)
        classToTest.setFrameListener(frameListener)
        val frameCallback = classToTest.frameCallback

        frameCallback.doFrame(FRAME_NANO_TIME)

        verify { frameListener.invoke(FRAME_NANO_TIME) }
        verify(exactly = 2) { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun doFrame_should_not_re_register_when_fpsActive_is_false() {
        classToTest.start() // fpsActive = true, posts callback once
        classToTest.stop() // fpsActive = false, removes callback

        classToTest.frameCallback.doFrame(FRAME_NANO_TIME)

        // postFrameCallback was called exactly once by start(), never by doFrame after stop
        verify(exactly = 1) { choreographer.postFrameCallback(any()) }
    }

    @Test
    fun doFrame_should_survive_listener_exception_and_re_register() {
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
