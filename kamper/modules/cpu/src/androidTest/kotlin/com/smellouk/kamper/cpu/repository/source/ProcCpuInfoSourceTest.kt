package com.smellouk.kamper.cpu.repository.source

import android.os.Process
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ProcCpuInfoSourceTest {
    private val logger = mockk<Logger>(relaxed = true)
    private val classToTest: ProcCpuInfoSource = ProcCpuInfoSource(logger)

    @Before
    fun setup() {
        mockkStatic(Process::class)
        every { Process.myPid() } returns PID

        mockkObject(ProcFileReader)
    }

    @After
    fun tearDown() {
        unmockkStatic(Process::class)
        unmockkObject(ProcFileReader)
    }

    @Test
    fun `getCpuInfoDto should return valid cpu info dto`() {
        every { ProcFileReader.getCpuProcStatTime() } returnsMany listOf(
            CPU_DEVICE_RATE,
            CPU_DEVICE_RATE2
        )
        every { ProcFileReader.getCpuProcPidStatTime(PID) } returnsMany listOf(
            CPU_APP_RATE,
            CPU_APP_RATE2
        )

        classToTest.getCpuInfoDto()
        val info = classToTest.getCpuInfoDto()

        assertEquals(EXPECTED_INFO_FINAL, info)
    }

    @Test
    fun `parse should parse cpu info strings to the expected cpu info object`() {
        val info = classToTest.parse(
            CPU_DEVICE_RATE,
            CPU_APP_RATE
        )

        assertEquals(EXPECTED_INFO, info)
    }

    @Test
    fun `parse should return invalid cpu info when cpu device string is invalid`() {
        val info = classToTest.parse(
            ANY_CPU_DEVICE_RATE,
            CPU_APP_RATE
        )

        assertEquals(CpuInfoDto.INVALID, info)
    }

    @Test
    fun `parse should return invalid cpu info when cpu app string is invalid`() {
        val info = classToTest.parse(
            CPU_DEVICE_RATE,
            ANY_CPU_APP_RATE
        )

        assertEquals(CpuInfoDto.INVALID, info)
    }
}

private const val PID = 123
private const val ANY_CPU_DEVICE_RATE = "ANY_CPU_APP_RATE"
private const val ANY_CPU_APP_RATE = "ANY_CPU_APP_RATE"

private const val CPU_DEVICE_RATE = "cpu  111028 13022 114874 19231064 1541 1140 1777 0 0 0"
private const val CPU_APP_RATE =
    "9352 (.kamper.samples) S 1183 1183 0 0 -1 4219200 15913 0 0 0 30 14 0 0 20 0 19 0 4903200 " +
            "1332465664 12371 18446744073709551615 140602116550656 140602116565416 140737082570752 " +
            "140737082566888 140602113705530 0 4612 0 38136 18446744073709551615 0 0 17 3 0 0 0 0 " +
            "0 140602116569760 140602116571112 140602118877184 140737082575881 140737082575980 " +
            "140737082575980 140737082576862 0"

private const val CPU_DEVICE_RATE2 = "cpu  111031 13022 114876 19231449 1541 1140 1777 0 0 0"
private const val CPU_APP_RATE2 =
    "9352 (.kamper.samples) S 1183 1183 0 0 -1 1077961024 16056 0 0 0 33 16 0 0 20 0 19 0 4903200 " +
            "1332506624 12491 18446744073709551615 140602116550656 140602116565416 140737082570752 " +
            "140737082566888 140602113705530 0 4612 0 38136 18446744073709551615 0 0 17 3 0 0 0 0 " +
            "0 140602116569760 140602116571112 140602118877184 140737082575881 140737082575980 " +
            "140737082575980 140737082576862 0"

private val EXPECTED_INFO = CpuInfoDto(
    totalTime = 19474446.0,
    userTime = 111028.0,
    systemTime = 114874.0,
    idleTime = 19231064.0,
    ioWaitTime = 1541.0,
    appTime = 44.0
)

private val EXPECTED_INFO_2 = CpuInfoDto(
    totalTime = 19474836.0,
    userTime = 111031.0,
    systemTime = 114876.0,
    idleTime = 19231449.0,
    ioWaitTime = 1541.0,
    appTime = 49.0
)

private val EXPECTED_INFO_FINAL = CpuInfoDto(
    totalTime = EXPECTED_INFO_2.totalTime - EXPECTED_INFO.totalTime,
    userTime = EXPECTED_INFO_2.userTime - EXPECTED_INFO.userTime,
    systemTime = EXPECTED_INFO_2.systemTime - EXPECTED_INFO.systemTime,
    idleTime = EXPECTED_INFO_2.idleTime - EXPECTED_INFO.idleTime,
    ioWaitTime = EXPECTED_INFO_2.ioWaitTime - EXPECTED_INFO.ioWaitTime,
    appTime = EXPECTED_INFO_2.appTime - EXPECTED_INFO.appTime
)
