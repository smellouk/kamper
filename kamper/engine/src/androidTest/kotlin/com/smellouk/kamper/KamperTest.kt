package com.smellouk.kamper

import com.smellouk.kamper.api.Logger
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verifyNoMoreCalls
import org.junit.Test
import kotlin.test.assertEquals

class KamperTest {
    private val classToTest = spy(Kamper).also {
        every { it.start() } returns Unit
        every { it.stop() } returns Unit
        every { it.clear() } returns Unit
    }

    @Test
    fun `setup should override default config`() {
        val logger = mock<Logger>()

        classToTest.setup {
            this.logger = logger
        }

        assertEquals(logger, classToTest.config.logger)
    }

    @Test
    fun `start should call engine start`() {
        classToTest.start()

        verify { classToTest.start() }
        verifyNoMoreCalls(classToTest)
    }

    @Test
    fun `stop should call engine stop`() {
        classToTest.stop()

        verify { classToTest.stop() }
        verifyNoMoreCalls(classToTest)
    }

    @Test
    fun `clear should call engine clear`() {
        classToTest.clear()

        verify { classToTest.clear() }
        verifyNoMoreCalls(classToTest)
    }
}
