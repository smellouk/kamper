package com.smellouk.konitor.jank

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class JankInfoSentinelTest {

    @Test
    fun `INVALID should use typed -1 per field`() {
        with(JankInfo.INVALID) {
            assertEquals(-1, droppedFrames)
            assertEquals(-1f, jankyFrameRatio)
            assertEquals(-1L, worstFrameMs)
        }
    }

    @Test
    fun `UNSUPPORTED should use typed -2 per field`() {
        with(JankInfo.UNSUPPORTED) {
            assertEquals(-2, droppedFrames)
            assertEquals(-2f, jankyFrameRatio)
            assertEquals(-2L, worstFrameMs)
        }
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(JankInfo.INVALID, JankInfo.UNSUPPORTED)
    }
}
