package com.smellouk.kamper.fps.repository.source

import com.smellouk.kamper.api.nanosToSeconds
import com.smellouk.kamper.fps.repository.FpsInfoDto
import dev.mokkery.matcher.any
import dev.mokkery.spy
import dev.mokkery.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FpsInfoSourceTest {
    private val choreographer = spy(FpsChoreographer)

    private lateinit var classToTest: FpsInfoSource
    private lateinit var frameListener: FpsChoreographerFrameListener

    @Before
    fun setup() {
        classToTest = FpsInfoSource(choreographer)
        frameListener = classToTest.frameListener
    }

    @Test
    fun instance_creation_should_set_frame_listener() {
        verify { choreographer.setFrameListener(any()) }
    }

    @Test
    fun getFpsInfoDto_should_return_valid_fps_info() {
        frameListener.invoke(FRAME_NANO_TIME)
        frameListener.invoke(FRAME_NANO_TIME_2)

        val dto = classToTest.getFpsInfoDto()

        assertEquals(EXPECTED_FPS, dto)
    }

    @Test
    fun getFpsInfoDto_should_return_invalid_fps_info_when_currentFrameCount_equals_0() {
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
