package com.smellouk.konitor.gpu

import com.smellouk.konitor.gpu.repository.KgslAccessibilityProvider
import org.junit.Test
import kotlin.test.assertFalse

@Suppress("IllegalIdentifier")
class KgslAccessibilityProviderTest {

    @Test
    fun `isAccessible returns false on hosts without sysfs kgsl path`() {
        // Robolectric/JVM host has no /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage
        // → probe must catch the exception and return false (D-05).
        assertFalse(KgslAccessibilityProvider.isAccessible())
    }
}
