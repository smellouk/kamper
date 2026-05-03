package com.smellouk.konitor.gpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class GpuInfoUnsupportedTest {

    @Test
    fun `UNSUPPORTED should have all fields set to -2_0`() {
        with(GpuInfo.UNSUPPORTED) {
            assertEquals(-2.0, utilization)
            assertEquals(-2.0, usedMemoryMb)
            assertEquals(-2.0, totalMemoryMb)
            assertEquals(-2.0, appUtilization)
            assertEquals(-2.0, rendererUtilization)
            assertEquals(-2.0, tilerUtilization)
            assertEquals(-2.0, computeUtilization)
        }
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(GpuInfo.INVALID, GpuInfo.UNSUPPORTED)
        assertFalse(GpuInfo.INVALID === GpuInfo.UNSUPPORTED)
    }
}
