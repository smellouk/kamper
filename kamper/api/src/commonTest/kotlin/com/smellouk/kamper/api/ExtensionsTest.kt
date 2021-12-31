package com.smellouk.kamper.api

import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class ExtensionsTest {
    @Test
    fun `bytesToMb should return the expected correct value`() {
        val expectedMb = 1F

        assertEquals(expectedMb, 1048576L.bytesToMb())
    }

    @Test
    fun `kBytesToMb should return the expected correct value`() {
        val expectedMb = 1F

        assertEquals(expectedMb, 1024L.kBytesToMb())
    }

    @Test
    fun `nanosToSeconds should return the expected correct value`() {
        val expectedMb = 1.0

        assertEquals(expectedMb, 1000000000L.nanosToSeconds())
    }
}
