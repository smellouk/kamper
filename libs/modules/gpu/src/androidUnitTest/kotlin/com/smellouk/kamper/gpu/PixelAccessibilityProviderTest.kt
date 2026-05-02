package com.smellouk.kamper.gpu

import com.smellouk.kamper.gpu.repository.PixelAccessibilityProvider
import org.junit.Test
import kotlin.test.assertFalse

@Suppress("IllegalIdentifier")
class PixelAccessibilityProviderTest {

    @Test
    fun `isAccessible returns false on hosts without pixel gpu sysfs`() {
        assertFalse(PixelAccessibilityProvider.isAccessible())
    }
}
