package com.smellouk.kamper.thermal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

@Suppress("IllegalIdentifier")
class ThermalInfoSentinelTest {

    @Test
    fun `INVALID should have UNKNOWN thermal state and false throttling`() {
        assertEquals(ThermalState.UNKNOWN, ThermalInfo.INVALID.state)
        assertFalse(ThermalInfo.INVALID.isThrottling)
    }

    @Test
    fun `UNSUPPORTED should have UNSUPPORTED thermal state`() {
        assertEquals(ThermalState.UNSUPPORTED, ThermalInfo.UNSUPPORTED.state)
    }

    @Test
    fun `INVALID and UNSUPPORTED must not be equal`() {
        assertNotEquals(ThermalInfo.INVALID, ThermalInfo.UNSUPPORTED)
    }
}
