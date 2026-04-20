package com.smellouk.kamper

import com.smellouk.kamper.api.Logger
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class KamperConfigBuilderTest {
    @Test
    fun `build should build correct config`() {
        val logger = mock<Logger>()

        val config = KamperConfig.Builder.apply { this.logger = logger }.build()

        assertEquals(logger, config.logger)
    }
}
