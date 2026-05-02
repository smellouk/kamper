package com.smellouk.kamper.gpu

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class GpuInfoSentinelTest {

    @Test
    fun `INVALID should have all Double fields set to -1_0`() {
        with(GpuInfo.INVALID) {
            assertEquals(-1.0, utilization)
            assertEquals(-1.0, usedMemoryMb)
            assertEquals(-1.0, totalMemoryMb)
            assertEquals(-1.0, appUtilization)
            assertEquals(-1.0, rendererUtilization)
            assertEquals(-1.0, tilerUtilization)
            assertEquals(-1.0, computeUtilization)
        }
    }
}
