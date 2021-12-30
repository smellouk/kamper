package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Logger
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Suppress("IllegalIdentifier")
class FpsConfigBuilderTest {
    @Test
    fun `build should build correct config`() {
        val loggerMock = mockk<Logger>()
        with(FpsConfig.Builder.apply {
            isEnabled = false
            logger = loggerMock
        }.build()) {
            assertFalse(isEnabled)
            assertEquals(INTERVAL_IN_MS, intervalInMs)
            assertEquals(loggerMock, logger)
        }
    }
}

private const val INTERVAL_IN_MS = 1000L
