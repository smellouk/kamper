package com.smellouk.konitor.gpu

import com.smellouk.konitor.gpu.repository.PixelAccessibilityProvider
import org.junit.Test
import kotlin.test.assertFalse

@Suppress("IllegalIdentifier")
class PixelAccessibilityProviderTest {

    @Test
    fun `isAccessible returns false on hosts without pixel gpu sysfs`() {
        assertFalse(PixelAccessibilityProvider.isAccessible())
    }
}
