package com.smellouk.kamper

import com.smellouk.kamper.api.Logger
import dev.mokkery.mock
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class KamperTest {

    @After
    fun teardown() {
        Kamper.clear()
        Kamper.setup { }
    }

    @Test
    fun setup_shouldOverrideDefaultConfig() {
        val logger = mock<Logger>()

        Kamper.setup { this.logger = logger }

        assertEquals(logger, Kamper.config.logger)
    }

    @Test
    fun start_shouldNotThrow() {
        Kamper.start()
    }

    @Test
    fun stop_shouldNotThrow() {
        Kamper.stop()
    }

    @Test
    fun clear_shouldNotThrow() {
        Kamper.clear()
    }
}
