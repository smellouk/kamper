package com.smellouk.kamper

import com.smellouk.kamper.api.Logger
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class KamperTest {
    private val classToTest = spyk<Kamper>().apply {
        every { (this@apply as Engine).start() } returns Unit
        every { (this@apply as Engine).stop() } returns Unit
        every { (this@apply as Engine).clear() } returns Unit
    }

    private val parentEngine = (classToTest as Engine).also {
        every { it.start() } returns Unit
        every { it.stop() } returns Unit
        every { it.clear() } returns Unit
    }

    @Test
    fun `setup should override default config`() {
        val logger = mockk<Logger>()

        classToTest.setup {
            this.logger = logger
        }

        assertEquals(logger, classToTest.config.logger)
    }

    @Test
    fun `start should call engine start`() {
        classToTest.start()

        verify { parentEngine.start() }
        confirmVerified(parentEngine)
    }

    @Test
    fun `stop should call engine stop`() {
        classToTest.stop()

        verify { parentEngine.stop() }
        confirmVerified(parentEngine)
    }

    @Test
    fun `clear should call engine clear`() {
        classToTest.clear()

        verify { parentEngine.clear() }
        confirmVerified(parentEngine)
    }
}
