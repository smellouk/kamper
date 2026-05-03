package com.smellouk.konitor.gpu

import com.smellouk.konitor.gpu.repository.DevfreqAccessibilityProvider
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

@Suppress("IllegalIdentifier")
class DevfreqAccessibilityProviderTest {

    @Test
    fun `isAccessible returns false on hosts without any mali sysfs path`() {
        // JVM host has no /sys/class/devfreq/, /sys/class/misc/mali0/, or
        // /sys/bus/platform/drivers/mali/ → all three probes must return null.
        assertFalse(DevfreqAccessibilityProvider.isAccessible())
    }

    @Test
    fun `findMaliDir returns null on hosts without any mali sysfs path`() {
        assertNull(DevfreqAccessibilityProvider.findMaliDir())
    }
}
