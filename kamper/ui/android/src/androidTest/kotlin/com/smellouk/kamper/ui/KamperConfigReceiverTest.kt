package com.smellouk.kamper.ui

import android.content.Context
import android.content.Intent
import com.smellouk.kamper.Kamper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class KamperConfigReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val classToTest = KamperConfigReceiver()

    @Before
    fun setup() {
        mockkObject(Kamper)
        every { Kamper.start() } returns Unit
        every { Kamper.stop() } returns Unit
    }

    @After
    fun teardown() {
        unmockkObject(Kamper)
    }

    @Test
    fun onReceive_shouldInvokeKamperStart_whenEnabledExtraIsTrue() {
        val intent = Intent(ACTION).putExtra(EXTRA_ENABLED, true)

        classToTest.onReceive(context, intent)

        verify(exactly = 1) { Kamper.start() }
        verify(exactly = 0) { Kamper.stop() }
    }

    @Test
    fun onReceive_shouldInvokeKamperStop_whenEnabledExtraIsFalse() {
        val intent = Intent(ACTION).putExtra(EXTRA_ENABLED, false)

        classToTest.onReceive(context, intent)

        verify(exactly = 1) { Kamper.stop() }
        verify(exactly = 0) { Kamper.start() }
    }

    @Test
    fun onReceive_shouldDefaultToStart_whenEnabledExtraIsMissing() {
        val intent = Intent(ACTION)  // no extras

        classToTest.onReceive(context, intent)

        verify(exactly = 1) { Kamper.start() }
        verify(exactly = 0) { Kamper.stop() }
    }
}

private const val ACTION = "com.smellouk.kamper.CONFIGURE"
private const val EXTRA_ENABLED = "enabled"
