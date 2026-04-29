// Branch taken: UNSUPPORTED companion exists (NetworkInfo.kt read at commit c3a5c12).
// NetworkInfo has both NOT_SUPPORTED (-100F legacy) and UNSUPPORTED (-2F from Phase 7/FEAT-01).
// Tests cover INVALID, UNSUPPORTED (Phase 7 branch A), and legacy NOT_SUPPORTED sentinel.
package com.smellouk.kamper.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class NetworkInfoSentinelTest {

    @Test
    fun `INVALID should have all Float fields set to -1F`() {
        with(NetworkInfo.INVALID) {
            assertEquals(-1F, rxSystemTotalInMb)
            assertEquals(-1F, txSystemTotalInMb)
            assertEquals(-1F, rxAppInMb)
            assertEquals(-1F, txAppInMb)
        }
    }

    @Test
    fun `UNSUPPORTED should have all Float fields set to -2F`() {
        with(NetworkInfo.UNSUPPORTED) {
            assertEquals(-2F, rxSystemTotalInMb)
            assertEquals(-2F, txSystemTotalInMb)
            assertEquals(-2F, rxAppInMb)
            assertEquals(-2F, txAppInMb)
        }
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(NetworkInfo.INVALID, NetworkInfo.UNSUPPORTED)
    }

    @Test
    fun `NOT_SUPPORTED should have all Float fields set to -100F`() {
        with(NetworkInfo.NOT_SUPPORTED) {
            assertEquals(-100F, rxSystemTotalInMb)
            assertEquals(-100F, txSystemTotalInMb)
            assertEquals(-100F, rxAppInMb)
            assertEquals(-100F, txAppInMb)
        }
    }

    @Test
    fun `INVALID and NOT_SUPPORTED must not be equal`() {
        assertNotEquals(NetworkInfo.INVALID, NetworkInfo.NOT_SUPPORTED)
    }
}
