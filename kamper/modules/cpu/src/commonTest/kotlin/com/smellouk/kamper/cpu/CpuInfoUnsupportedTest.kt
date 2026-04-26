package com.smellouk.kamper.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class CpuInfoUnsupportedTest {
    @Test
    fun `UNSUPPORTED should be structurally distinct from INVALID`() {
        assertNotEquals(CpuInfo.INVALID, CpuInfo.UNSUPPORTED)
    }

    @Test
    fun `UNSUPPORTED should use -2_0 sentinel for every numeric field`() {
        with(CpuInfo.UNSUPPORTED) {
            assertEquals(-2.0, totalUseRatio)
            assertEquals(-2.0, appRatio)
            assertEquals(-2.0, userRatio)
            assertEquals(-2.0, systemRatio)
            assertEquals(-2.0, ioWaitRatio)
        }
    }

    @Test
    fun `INVALID should still use -1_0 sentinel after the change`() {
        with(CpuInfo.INVALID) {
            assertEquals(-1.0, totalUseRatio)
            assertEquals(-1.0, appRatio)
            assertEquals(-1.0, userRatio)
            assertEquals(-1.0, systemRatio)
            assertEquals(-1.0, ioWaitRatio)
        }
    }
}
