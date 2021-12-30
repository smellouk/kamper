package com.smellouk.kamper.fps.respository

import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.repository.FpsInfoDto
import com.smellouk.kamper.fps.repository.FpsInfoMapper
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class FpsInfoMapperTest {
    private val classToTest = FpsInfoMapper()

    @Test
    fun `map dto should return invalid fps info when dot is invalid`() {
        val fpsInfo = classToTest.map(FpsInfoDto.INVALID)

        assertEquals(FpsInfo.INVALID, fpsInfo)
    }

    @Test
    fun `map dto should return invalid fps info when currentFrameCount bellow 1`() {
        val dto = mockk<FpsInfoDto>().apply {
            every { currentFrameCount } returns WRONG_CURRENT_FRAME_COUNT
        }

        val fpsInfo = classToTest.map(dto)

        assertEquals(FpsInfo.INVALID, fpsInfo)
    }

    @Test
    fun `map dto should return invalid fps info when currentFrameCount bellow startFrameTimeInSeconds`() {
        val dto = FpsInfoDto(
            currentFrameCount = CURRENT_FRAME_COUNT,
            currentFrameTimeInSeconds = CURRENT_FRAME_TIME,
            startFrameTimeInSeconds = WRONG_START_FRAME_TIME
        )

        val fpsInfo = classToTest.map(dto)

        assertEquals(FpsInfo.INVALID, fpsInfo)
    }

    @Test
    fun `map dto should return valid fps info`() {
        val dto = FpsInfoDto(
            currentFrameCount = CURRENT_FRAME_COUNT,
            currentFrameTimeInSeconds = CURRENT_FRAME_TIME,
            startFrameTimeInSeconds = START_FRAME_TIME
        )

        val fpsInfo = classToTest.map(dto)

        assertEquals(EXPECTED_FPS, fpsInfo)
    }
}

private const val CURRENT_FRAME_COUNT = 31
private const val CURRENT_FRAME_TIME = 13536.398074533
private const val START_FRAME_TIME = 13535.481407903
private val EXPECTED_FPS = FpsInfo(33)

private const val WRONG_CURRENT_FRAME_COUNT = -1
private const val WRONG_START_FRAME_TIME = CURRENT_FRAME_TIME + 1.0
