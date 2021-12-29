package com.smellouk.kamper

import com.smellouk.kamper.api.Logger
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class KamperConfigBuilderTest {
    @Test
    fun `build should build correct config`() {
        val logger = mockk<Logger>()

        val config = KamperConfig.Builder.apply { this.logger = logger }.build()

        assertEquals(logger, config.logger)
    }
}
