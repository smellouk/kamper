package com.smellouk.kamper.memory.repository

import com.smellouk.kamper.api.bytesToMb
import com.smellouk.kamper.memory.MemoryInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class MemoryInfoMapperTest {
    private val classToTest = MemoryInfoMapper()

    @Test
    fun `map dto should return invalid memory info when dto is invalid`() {
        val memoryInfo = classToTest.map(MemoryInfoDto.INVALID)

        assertEquals(MemoryInfo.INVALID, memoryInfo)
    }

    @Test
    fun `map should map dto to correct memory info`() {
        val memoryInfo = classToTest.map(DTO)

        with(memoryInfo) {
            val expectedMemoryInMb = MEMORY_IN_BYTE.bytesToMb()
            with(heapMemoryInfo) {
                assertEquals(expectedMemoryInMb, maxMemoryInMb)
                assertEquals(expectedMemoryInMb, allocatedInMb)
            }

            with(pssInfo) {
                assertEquals(expectedMemoryInMb, totalPssInMb)
                assertEquals(expectedMemoryInMb, dalvikPssInMb)
                assertEquals(expectedMemoryInMb, nativePssInMb)
                assertEquals(expectedMemoryInMb, otherPssInMb)
            }

            with(ramInfo) {
                assertEquals(expectedMemoryInMb, availableRamInMb)
                assertEquals(expectedMemoryInMb, totalRamInMb)
                assertEquals(expectedMemoryInMb, lowRamThresholdInMb)
                assertTrue(isLowMemory)
            }
        }
    }

    @Test
    fun `map should map dto to invalid pss info when no pss info available`() {
        val memoryInfo = classToTest.map(DTO_WITH_NULL_PSS)

        with(memoryInfo) {
            val expectedMemoryInMb = MEMORY_IN_BYTE.bytesToMb()
            with(pssInfo) {
                assertEquals(INVALID_MEMORY, totalPssInMb)
                assertEquals(INVALID_MEMORY, dalvikPssInMb)
                assertEquals(INVALID_MEMORY, nativePssInMb)
                assertEquals(INVALID_MEMORY, otherPssInMb)
            }
        }
    }
}

private const val INVALID_MEMORY = -1F
private const val MEMORY_IN_BYTE = 1024 * 1024L
private const val MEMORY_IN_KILo_BYTE = 1024L

private val DTO = MemoryInfoDto(
    maxMemoryInBytes = MEMORY_IN_BYTE,
    allocatedInBytes = MEMORY_IN_BYTE,
    totalPssInKiloBytes = MEMORY_IN_KILo_BYTE,
    dalvikPssInKiloBytes = MEMORY_IN_KILo_BYTE,
    nativePssInKiloBytes = MEMORY_IN_KILo_BYTE,
    otherPssInKiloBytes = MEMORY_IN_KILo_BYTE,
    availableRamInBytes = MEMORY_IN_BYTE,
    totalRamInBytes = MEMORY_IN_BYTE,
    lowRamThresholdInBytes = MEMORY_IN_BYTE,
    isLowMemory = true
)

private val DTO_WITH_NULL_PSS = MemoryInfoDto(
    maxMemoryInBytes = MEMORY_IN_BYTE,
    allocatedInBytes = MEMORY_IN_BYTE,
    totalPssInKiloBytes = null,
    dalvikPssInKiloBytes = null,
    nativePssInKiloBytes = null,
    otherPssInKiloBytes = null,
    availableRamInBytes = MEMORY_IN_BYTE,
    totalRamInBytes = MEMORY_IN_BYTE,
    lowRamThresholdInBytes = MEMORY_IN_BYTE,
    isLowMemory = true
)
