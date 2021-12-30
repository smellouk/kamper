package com.smellouk.kamper.memory

import com.smellouk.kamper.api.Logger
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Suppress("IllegalIdentifier")
class MemoryConfigBuilderTest {
    @Test
    fun `build should build correct config`() {
        val loggerMock = mockk<Logger>()
        with(MemoryConfig.Builder.DEFAULT.apply {
            isEnabled = false
            intervalInMs = INTERVAL_IN_MS
            logger = loggerMock
        }.build()) {
            assertFalse(isEnabled)
            assertEquals(INTERVAL_IN_MS, intervalInMs)
            assertEquals(loggerMock, logger)
        }
    }
}

private const val INTERVAL_IN_MS = 1000L
