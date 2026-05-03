package com.smellouk.konitor.ui

import android.content.Context
import android.content.Intent
import com.smellouk.konitor.Konitor
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class KonitorConfigReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val classToTest = KonitorConfigReceiver()

    @Before
    fun setup() {
        mockkObject(Konitor)
        every { Konitor.start() } returns Unit
        every { Konitor.stop() } returns Unit
    }

    @After
    fun teardown() {
        unmockkObject(Konitor)
    }

    @Test
    fun onReceive_shouldInvokeKonitorStart_whenEnabledExtraIsTrue() {
        val intent = Intent(ACTION).putExtra(EXTRA_ENABLED, true)

        classToTest.onReceive(context, intent)

        verify(exactly = 1) { Konitor.start() }
        verify(exactly = 0) { Konitor.stop() }
    }

    @Test
    fun onReceive_shouldInvokeKonitorStop_whenEnabledExtraIsFalse() {
        val intent = Intent(ACTION).putExtra(EXTRA_ENABLED, false)

        classToTest.onReceive(context, intent)

        verify(exactly = 1) { Konitor.stop() }
        verify(exactly = 0) { Konitor.start() }
    }

    @Test
    fun onReceive_shouldDefaultToStart_whenEnabledExtraIsMissing() {
        val intent = Intent(ACTION)  // no extras

        classToTest.onReceive(context, intent)

        verify(exactly = 1) { Konitor.start() }
        verify(exactly = 0) { Konitor.stop() }
    }
}

private const val ACTION = "com.smellouk.konitor.CONFIGURE"
private const val EXTRA_ENABLED = "enabled"
