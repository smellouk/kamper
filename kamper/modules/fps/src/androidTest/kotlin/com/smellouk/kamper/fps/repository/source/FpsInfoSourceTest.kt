package com.smellouk.kamper.fps.repository.source

import com.smellouk.kamper.api.nanosToSeconds
import com.smellouk.kamper.fps.repository.FpsInfoDto
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FpsInfoSourceTest {
    private val choreographer = mockk<FpsChoreographer>(relaxed = true)

    private lateinit var classToTest: FpsInfoSource
    private lateinit var frameListener: FpsChoreographerFrameListener

    @Before
    fun setup() {
        classToTest = FpsInfoSource(choreographer)
        frameListener = classToTest.frameListener
    }

    @Test
    fun `instance creation should set frame listener`() {
        verify { choreographer.setFrameListener(any()) }
    }

    @Test
    fun `getFpsInfoDto should return valid fps info`() {
        frameListener.invoke(FRAME_NANO_TIME)
        frameListener.invoke(FRAME_NANO_TIME_2)

        val dto = classToTest.getFpsInfoDto()

        assertEquals(EXPECTED_FPS, dto)
    }

    @Test
    fun `getFpsInfoDto should return invalid fps info when currentFrameCount equals 0`() {
        val dto = classToTest.getFpsInfoDto()

        assertEquals(FpsInfoDto.INVALID, dto)
    }
}

private const val FRAME_NANO_TIME = 1000000000L
private const val FRAME_NANO_TIME_2 = 2000000000L
private val EXPECTED_FPS = FpsInfoDto(
    currentFrameCount = 2,
    startFrameTimeInSeconds = FRAME_NANO_TIME.nanosToSeconds(),
    currentFrameTimeInSeconds = FRAME_NANO_TIME_2.nanosToSeconds()
)
