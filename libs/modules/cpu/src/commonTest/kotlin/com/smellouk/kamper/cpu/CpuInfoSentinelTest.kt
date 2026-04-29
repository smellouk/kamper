package com.smellouk.kamper.cpu

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class CpuInfoSentinelTest {

    @Test
    fun `INVALID should have all Double fields set to -1_0`() {
        with(CpuInfo.INVALID) {
            assertEquals(-1.0, totalUseRatio)
            assertEquals(-1.0, appRatio)
            assertEquals(-1.0, userRatio)
            assertEquals(-1.0, systemRatio)
            assertEquals(-1.0, ioWaitRatio)
        }
    }
}
