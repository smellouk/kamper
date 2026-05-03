package com.smellouk.konitor.gc

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class GcInfoSentinelTest {

    @Test
    fun `INVALID should have all Long fields set to -1L`() {
        with(GcInfo.INVALID) {
            assertEquals(-1L, gcCount)
            assertEquals(-1L, gcPauseMs)
            assertEquals(-1L, gcCountDelta)
            assertEquals(-1L, gcPauseMsDelta)
        }
    }

    @Test
    fun `UNSUPPORTED should have all Long fields set to -2L`() {
        with(GcInfo.UNSUPPORTED) {
            assertEquals(-2L, gcCount)
            assertEquals(-2L, gcPauseMs)
            assertEquals(-2L, gcCountDelta)
            assertEquals(-2L, gcPauseMsDelta)
        }
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(GcInfo.INVALID, GcInfo.UNSUPPORTED)
    }
}
