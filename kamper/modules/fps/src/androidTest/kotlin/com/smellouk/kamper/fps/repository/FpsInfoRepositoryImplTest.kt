package com.smellouk.kamper.fps.repository

import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.repository.source.FpsInfoSource
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class FpsInfoRepositoryImplTest {
    private val fpsInfoDto = mockk<FpsInfoDto>()
    private val fpsInfoSource = mockk<FpsInfoSource>().apply {
        every { getFpsInfoDto() } returns fpsInfoDto
    }

    private val fpsInfo = mockk<FpsInfo>()
    private val fpsInfoMapper = mockk<FpsInfoMapper>().apply {
        every { map(fpsInfoDto) } returns fpsInfo
    }

    private val classToTest: FpsInfoRepositoryImpl by lazy {
        FpsInfoRepositoryImpl(
            fpsInfoSource,
            fpsInfoMapper
        )
    }

    @Test
    fun `getInfo should get fps info`() {
        val fpsInfo = classToTest.getInfo()

        assertEquals(this.fpsInfo, fpsInfo)
        verify { fpsInfoSource.getFpsInfoDto() }
        verify { fpsInfoMapper.map(fpsInfoDto) }
        confirmVerified(fpsInfoSource, fpsInfoMapper)
    }
}
