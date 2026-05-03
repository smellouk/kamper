package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.Logger
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@Suppress("IllegalIdentifier")
class GpuConfigBuilderTest {

    @Test
    fun `build should build correct config`() {
        val loggerMock = mock<Logger>()
        with(
            GpuConfig.Builder().apply {
                isEnabled = false
                intervalInMs = INTERVAL_IN_MS
                logger = loggerMock
            }.build()
        ) {
            assertFalse(isEnabled)
            assertEquals(INTERVAL_IN_MS, intervalInMs)
            assertEquals(loggerMock, logger)
        }
    }
}

private const val INTERVAL_IN_MS = 1000L
