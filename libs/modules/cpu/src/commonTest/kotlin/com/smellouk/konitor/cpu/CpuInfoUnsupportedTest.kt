package com.smellouk.konitor.cpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class CpuInfoUnsupportedTest {

    @Test
    fun `UNSUPPORTED should have all fields set to -2`() {
        with(CpuInfo.UNSUPPORTED) {
            assertEquals(-2.0, totalUseRatio)
            assertEquals(-2.0, appRatio)
            assertEquals(-2.0, userRatio)
            assertEquals(-2.0, systemRatio)
            assertEquals(-2.0, ioWaitRatio)
        }
    }

    @Test
    fun `INVALID should have all fields set to -1`() {
        with(CpuInfo.INVALID) {
            assertEquals(-1.0, totalUseRatio)
            assertEquals(-1.0, appRatio)
            assertEquals(-1.0, userRatio)
            assertEquals(-1.0, systemRatio)
            assertEquals(-1.0, ioWaitRatio)
        }
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(CpuInfo.INVALID, CpuInfo.UNSUPPORTED)
        assertFalse(CpuInfo.INVALID === CpuInfo.UNSUPPORTED)
    }
}
