package com.smellouk.konitor.fps.repository

import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.repository.source.FpsInfoSource
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifyNoMoreCalls
import org.junit.Test
import kotlin.test.assertEquals

class FpsInfoRepositoryImplTest {
    private val fpsInfoDto = mock<FpsInfoDto>()
    private val fpsInfoSource = mock<FpsInfoSource>().also {
        every { it.getFpsInfoDto() } returns fpsInfoDto
    }

    private val fpsInfo = mock<FpsInfo>()
    private val fpsInfoMapper = mock<FpsInfoMapper>().also {
        every { it.map(fpsInfoDto) } returns fpsInfo
    }

    private val classToTest: FpsInfoRepositoryImpl by lazy {
        FpsInfoRepositoryImpl(fpsInfoSource, fpsInfoMapper)
    }

    @Test
    fun getInfo_should_get_fps_info() {
        val fpsInfo = classToTest.getInfo()

        assertEquals(this.fpsInfo, fpsInfo)
        verify { fpsInfoSource.getFpsInfoDto() }
        verify { fpsInfoMapper.map(fpsInfoDto) }
        verifyNoMoreCalls(fpsInfoSource, fpsInfoMapper)
    }
}
