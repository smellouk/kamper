package com.smellouk.kamper.cpu.repository

import com.smellouk.kamper.cpu.CpuInfo
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class CpuInfoMapperTest {
    private val classToTest = CpuInfoMapper()

    @Test
    fun `map dto should return invalid cpu info when dto is invalid`() {
        assertEquals(CpuInfo.INVALID, classToTest.map(CpuInfoDto.INVALID))
    }

    @Test
    fun `map dto should return invalid cpu info when total is negative`() {
        val dto = mockk<CpuInfoDto>().apply {
            every { totalTime } returns -1.0
        }

        val info = classToTest.map(dto)

        assertEquals(CpuInfo.INVALID, info)
    }

    @Test
    fun `map dto should map data to CpuInfo object`() {
        val dto = CpuInfoDto(
            userTime = USER_CPU,
            systemTime = SYSTEM_CPU,
            idleTime = IDLE_CPU,
            ioWaitTime = IO_WAIT_CPU,
            totalTime = TOTAL_CPU,
            appTime = APP_CPU
        )

        val info = classToTest.map(dto)

        with(info) {
            val expectedTotalRation = (TOTAL_CPU - IDLE_CPU) / TOTAL_CPU.toDouble()
            assertEquals(expectedTotalRation, totalUseRatio)

            val expectedAppRation = APP_CPU / TOTAL_CPU.toDouble()
            assertEquals(expectedAppRation, appRatio)

            val expectedUserRation = USER_CPU / TOTAL_CPU.toDouble()
            assertEquals(expectedUserRation, userRatio)

            val expectedSystemRation = SYSTEM_CPU / TOTAL_CPU.toDouble()
            assertEquals(expectedSystemRation, systemRatio)

            val expectedIoWaitRation = IO_WAIT_CPU / TOTAL_CPU.toDouble()
            assertEquals(expectedIoWaitRation, ioWaitRatio)
        }
    }
}

private const val TOTAL_CPU = 10.0
private const val IDLE_CPU = 2.0
private const val APP_CPU = 1.0
private const val USER_CPU = 3.0
private const val SYSTEM_CPU = 4.0
private const val IO_WAIT_CPU = 5.0
