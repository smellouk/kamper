package com.smellouk.konitor

import com.smellouk.konitor.api.Logger
import dev.mokkery.mock
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class KonitorConfigBuilderTest {
    @Test
    fun `build should build correct config`() {
        val logger = mock<Logger>()

        val config = KonitorConfig.Builder().apply { this.logger = logger }.build()

        assertEquals(logger, config.logger)
    }
}
