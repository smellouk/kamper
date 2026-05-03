package com.smellouk.konitor

import com.smellouk.konitor.api.Logger
import dev.mokkery.mock
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class KonitorTest {

    @After
    fun teardown() {
        Konitor.clear()
        Konitor.setup { }
    }

    @Test
    fun setup_shouldOverrideDefaultConfig() {
        val logger = mock<Logger>()

        Konitor.setup { this.logger = logger }

        assertEquals(logger, Konitor.config.logger)
    }

    @Test
    fun start_shouldNotThrow() {
        Konitor.start()
    }

    @Test
    fun stop_shouldNotThrow() {
        Konitor.stop()
    }

    @Test
    fun clear_shouldNotThrow() {
        Konitor.clear()
    }
}
