package com.smellouk.kamper.cpu.repository.source

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class ShellCpuInfoSourceTest {
    private val logger = mockk<Logger>(relaxed = true)

    private val classToTest: ShellCpuInfoSource = ShellCpuInfoSource(logger)

    @Before
    fun setup() {
        mockkStatic(Runtime::class)
        mockkStatic(android.os.Process::class)
        every { android.os.Process.myPid() } returns PID
        mockkObject(ShellProcFileReader)
    }

    @After
    fun tearDown() {
        unmockkObject(ShellProcFileReader)
        unmockkStatic(android.os.Process::class)
        unmockkStatic(Runtime::class)
    }

    @Test
    fun `getCpuInfoDto never invokes Runtime exec`() {
        stubReads(PROC_STAT_LINE_1, PROC_PID_STAT_LINE_1)

        classToTest.getCpuInfoDto()

        verify(exactly = 0) { Runtime.getRuntime().exec(any<String>()) }
    }

    @Test
    fun `getCpuInfoDto returns INVALID on first call before any cached snapshot exists`() {
        stubReads(PROC_STAT_LINE_1, PROC_PID_STAT_LINE_1)

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
    }

    @Test
    fun `getCpuInfoDto returns valid delta on second call after cache is seeded`() {
        stubReads(PROC_STAT_LINE_1, PROC_PID_STAT_LINE_1)
        classToTest.getCpuInfoDto()

        stubReads(PROC_STAT_LINE_2, PROC_PID_STAT_LINE_2)
        val dto = classToTest.getCpuInfoDto()

        assertNotEquals(CpuInfoDto.INVALID, dto)
        assertTrue(dto.totalTime >= 0.0)
        assertTrue(dto.userTime >= 0.0)
        assertTrue(dto.systemTime >= 0.0)
        assertTrue(dto.idleTime >= 0.0)
        assertTrue(dto.ioWaitTime >= 0.0)
    }

    @Test
    fun `getCpuInfoDto returns INVALID when proc stat read throws`() {
        every { ShellProcFileReader.getPidStatLine(PID) } returns PROC_PID_STAT_LINE_1
        every { ShellProcFileReader.getStatLine() } throws RuntimeException("SELinux: /proc/stat denied")

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
    }

    @Test
    fun `getCpuInfoDto computes correct deltas from two seeded readings`() {
        // PROC_STAT_LINE_1: user=100, nice=0, system=50, idle=800, iowait=10, total=965
        // PROC_STAT_LINE_2: user=200, nice=0, system=100, idle=1600, iowait=20, total=1930
        // Deltas: totalTime=965, userTime=100, systemTime=50, idleTime=800, ioWaitTime=10
        stubReads(PROC_STAT_LINE_1, PROC_PID_STAT_LINE_1)
        classToTest.getCpuInfoDto()

        stubReads(PROC_STAT_LINE_2, PROC_PID_STAT_LINE_2)
        val dto = classToTest.getCpuInfoDto()

        assertEquals(965.0, dto.totalTime, DELTA_TOLERANCE)
        assertEquals(100.0, dto.userTime, DELTA_TOLERANCE)
        assertEquals(50.0, dto.systemTime, DELTA_TOLERANCE)
        assertEquals(800.0, dto.idleTime, DELTA_TOLERANCE)
        assertEquals(10.0, dto.ioWaitTime, DELTA_TOLERANCE)
    }

    private fun stubReads(procStatLine: String, procPidStatLine: String) {
        every { ShellProcFileReader.getStatLine() } returns procStatLine
        every { ShellProcFileReader.getPidStatLine(PID) } returns procPidStatLine
    }
}

private const val PID = 11916
private const val DELTA_TOLERANCE = 0.001

// /proc/stat first line — note double space after "cpu" per kernel format:
// user=100, nice=0, system=50, idle=800, iowait=10, irq=0, softirq=0, steal=5; total=965
private const val PROC_STAT_LINE_1 = "cpu  100 0 50 800 10 0 0 5 0 0"

// user=200, nice=0, system=100, idle=1600, iowait=20, irq=0, softirq=0, steal=10; total=1930
// Deltas vs line 1: total=965, user=100, system=50, idle=800, iowait=10
private const val PROC_STAT_LINE_2 = "cpu  200 0 100 1600 20 0 0 10 0 0"

// /proc/[pid]/stat — indices 13-16 are utime/stime/cutime/cstime
private const val PROC_PID_STAT_LINE_1 =
    "11916 (kamper) S 1 1 0 0 -1 0 0 0 0 0 0 0 100 50 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"

private const val PROC_PID_STAT_LINE_2 =
    "11916 (kamper) S 1 1 0 0 -1 0 0 0 0 0 0 0 200 100 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0"
