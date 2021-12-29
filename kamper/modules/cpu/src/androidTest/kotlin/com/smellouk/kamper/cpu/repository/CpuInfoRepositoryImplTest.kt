package com.smellouk.kamper.cpu.repository

import com.smellouk.kamper.cpu.repository.source.CpuInfoSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class CpuInfoRepositoryImplTest {
    private val procCpuInfoRawSource = mockk<CpuInfoSource>()
    private val shellCpuInfoRawSource = mockk<CpuInfoSource>()
    private val cpuInfoMapper = mockk<CpuInfoMapper>(relaxed = true)

    private val classToTest: CpuInfoRepositoryImpl by lazy {
        CpuInfoRepositoryImpl(
            procCpuInfoRawSource = procCpuInfoRawSource,
            shellCpuInfoRawSource = shellCpuInfoRawSource,
            cpuInfoMapper = cpuInfoMapper
        )
    }

    @Before
    fun setup() {
        mockkObject(ApiLevelProvider)
    }

    @After
    fun tearDown() {
        unmockkObject(ApiLevelProvider)
    }

    @Test
    fun `getInfo should get it from procCpuInfoRawSource when api level bellow 26`() {
        val dto = mockk<CpuInfoDto>()
        every { ApiLevelProvider.getApiLevel() } returns 20
        every { procCpuInfoRawSource.getCpuInfoDto() } returns dto

        classToTest.getInfo()

        verify { procCpuInfoRawSource.getCpuInfoDto() }
        verify(exactly = 0) { shellCpuInfoRawSource.getCpuInfoDto() }
        verify { cpuInfoMapper.map(dto) }
    }

    @Test
    fun `getInfo should get it from shellCpuInfoRawSource when api level superior or equals to 26`() {
        val dto = mockk<CpuInfoDto>()
        every { ApiLevelProvider.getApiLevel() } returns 26
        every { shellCpuInfoRawSource.getCpuInfoDto() } returns dto

        classToTest.getInfo()

        verify { shellCpuInfoRawSource.getCpuInfoDto() }
        verify(exactly = 0) { procCpuInfoRawSource.getCpuInfoDto() }
        verify { cpuInfoMapper.map(dto) }
    }
}
