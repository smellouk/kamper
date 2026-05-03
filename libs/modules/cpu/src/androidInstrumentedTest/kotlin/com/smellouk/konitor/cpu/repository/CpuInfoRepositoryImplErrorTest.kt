package com.smellouk.konitor.cpu.repository

import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.repository.source.CpuInfoSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CpuInfoRepositoryImplErrorTest {

    private val procCpuInfoRawSource = mockk<CpuInfoSource>()
    private val shellCpuInfoRawSource = mockk<CpuInfoSource>()
    private val cpuInfoMapper = mockk<CpuInfoMapper>(relaxed = true)

    private val classToTest: CpuInfoRepositoryImpl by lazy {
        CpuInfoRepositoryImpl(
            procCpuInfoSource = procCpuInfoRawSource,
            shellCpuInfoSource = shellCpuInfoRawSource,
            cpuInfoMapper = cpuInfoMapper
        )
    }

    @Before
    fun setup() {
        mockkObject(ApiLevelProvider)
        mockkObject(ProcStatAccessibilityProvider)
    }

    @After
    fun tearDown() {
        unmockkObject(ApiLevelProvider)
        unmockkObject(ProcStatAccessibilityProvider)
    }

    @Test
    fun getInfo_shouldReturnINVALID_whenProcSourceReturnsINVALID_andApiLevelBelow26() {
        every { ApiLevelProvider.getApiLevel() } returns 20
        every { procCpuInfoRawSource.getCpuInfoDto() } returns CpuInfoDto.INVALID
        every { cpuInfoMapper.map(CpuInfoDto.INVALID) } returns CpuInfo.INVALID

        val result = classToTest.getInfo()

        assertEquals(CpuInfo.INVALID, result)
    }

    @Test
    fun getInfo_shouldReturnINVALID_whenShellSourceReturnsINVALID_andProcStatNotAccessible() {
        every { ApiLevelProvider.getApiLevel() } returns 26
        every { ProcStatAccessibilityProvider.isAccessible() } returns false
        every { shellCpuInfoRawSource.getCpuInfoDto() } returns CpuInfoDto.INVALID
        every { cpuInfoMapper.map(CpuInfoDto.INVALID) } returns CpuInfo.INVALID

        val result = classToTest.getInfo()

        assertEquals(CpuInfo.INVALID, result)
    }

    @Test
    fun getInfo_shouldReturnINVALID_whenProcSourceReturnsINVALID_atApi26WithAccessibleProcStat() {
        every { ApiLevelProvider.getApiLevel() } returns 26
        every { ProcStatAccessibilityProvider.isAccessible() } returns true
        every { procCpuInfoRawSource.getCpuInfoDto() } returns CpuInfoDto.INVALID
        every { cpuInfoMapper.map(CpuInfoDto.INVALID) } returns CpuInfo.INVALID

        val result = classToTest.getInfo()

        assertEquals(CpuInfo.INVALID, result)
    }
}
