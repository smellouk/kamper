package com.smellouk.kamper.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class MemoryInfoSentinelTest {

    @Test
    fun `INVALID should point at each nested INVALID sub-object`() {
        assertEquals(MemoryInfo.HeapMemoryInfo.INVALID, MemoryInfo.INVALID.heapMemoryInfo)
        assertEquals(MemoryInfo.PssInfo.INVALID, MemoryInfo.INVALID.pssInfo)
        assertEquals(MemoryInfo.RamInfo.INVALID, MemoryInfo.INVALID.ramInfo)
    }

    @Test
    fun `HeapMemoryInfo INVALID should use -1F for all Float fields`() {
        with(MemoryInfo.HeapMemoryInfo.INVALID) {
            assertEquals(-1F, maxMemoryInMb)
            assertEquals(-1F, allocatedInMb)
        }
    }

    @Test
    fun `PssInfo INVALID should use -1F for all Float fields`() {
        with(MemoryInfo.PssInfo.INVALID) {
            assertEquals(-1F, totalPssInMb)
            assertEquals(-1F, dalvikPssInMb)
            assertEquals(-1F, nativePssInMb)
            assertEquals(-1F, otherPssInMb)
        }
    }

    @Test
    fun `RamInfo INVALID should use -1F for Float fields and false for isLowMemory`() {
        with(MemoryInfo.RamInfo.INVALID) {
            assertEquals(-1F, availableRamInMb)
            assertEquals(-1F, totalRamInMb)
            assertEquals(-1F, lowRamThresholdInMb)
            assertFalse(isLowMemory)
        }
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(MemoryInfo.INVALID, MemoryInfo.UNSUPPORTED)
    }
}
