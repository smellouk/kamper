package com.smellouk.konitor.fps

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class FpsInfoSentinelTest {

    @Test
    fun `INVALID should have fps set to -1`() {
        assertEquals(-1, FpsInfo.INVALID.fps)
    }

    @Test
    fun `UNSUPPORTED should have fps set to -2`() {
        assertEquals(-2, FpsInfo.UNSUPPORTED.fps)
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(FpsInfo.INVALID, FpsInfo.UNSUPPORTED)
        assertFalse(FpsInfo.INVALID === FpsInfo.UNSUPPORTED)
    }
}
