package com.smellouk.konitor.cpu.repository.source

import android.os.Process
import android.os.SystemClock
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.cpu.repository.CpuInfoDto
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
import kotlin.test.assertNotEquals

class ShellCpuInfoSourceTest {
    private val logger = mockk<Logger>(relaxed = true)
    private val classToTest: ShellCpuInfoSource = ShellCpuInfoSource(logger)

    @Before
    fun setup() {
        mockkStatic(Process::class)
        every { Process.myPid() } returns PID

        mockkStatic(SystemClock::class)
        mockkObject(ProcFileReader)
        mockkStatic(Runtime::class)
        every { Runtime.getRuntime().availableProcessors() } returns NUM_CORES
    }

    @After
    fun tearDown() {
        unmockkStatic(Process::class)
        unmockkStatic(SystemClock::class)
        unmockkObject(ProcFileReader)
        unmockkStatic(Runtime::class)
    }

    @Test
    fun getCpuInfoDto_returns_INVALID_on_first_call_no_delta_yet() {
        every { ProcFileReader.getCpuProcPidStatTime(PID) } returns PID_STAT_SAMPLE_1
        every { SystemClock.elapsedRealtime() } returns 0L

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
    }

    @Test
    fun getCpuInfoDto_returns_valid_dto_with_correct_appTime_on_second_call() {
        every { ProcFileReader.getCpuProcPidStatTime(PID) } returnsMany listOf(
            PID_STAT_SAMPLE_1,
            PID_STAT_SAMPLE_2
        )
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, ELAPSED_MS)

        classToTest.getCpuInfoDto()  // first call — cache only
        val dto = classToTest.getCpuInfoDto()

        assertNotEquals(CpuInfoDto.INVALID, dto)
        assertEquals(EXPECTED_APP_DELTA, dto.appTime, DELTA_TOLERANCE)
    }

    @Test
    fun getCpuInfoDto_returns_correct_totalTime_based_on_cores_and_elapsed_time() {
        every { ProcFileReader.getCpuProcPidStatTime(PID) } returnsMany listOf(
            PID_STAT_SAMPLE_1,
            PID_STAT_SAMPLE_2
        )
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, ELAPSED_MS)

        classToTest.getCpuInfoDto()  // first call
        val dto = classToTest.getCpuInfoDto()

        // totalTicks = (elapsedMs / 10.0) * numCores = (1000 / 10.0) * 4 = 400
        assertEquals(EXPECTED_TOTAL_TICKS, dto.totalTime, DELTA_TOLERANCE)
    }

    @Test
    fun getCpuInfoDto_returns_idleTime_as_totalTime_minus_appDelta() {
        every { ProcFileReader.getCpuProcPidStatTime(PID) } returnsMany listOf(
            PID_STAT_SAMPLE_1,
            PID_STAT_SAMPLE_2
        )
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, ELAPSED_MS)

        classToTest.getCpuInfoDto()  // first call
        val dto = classToTest.getCpuInfoDto()

        assertEquals(EXPECTED_TOTAL_TICKS - EXPECTED_APP_DELTA, dto.idleTime, DELTA_TOLERANCE)
    }

    @Test
    fun getCpuInfoDto_returns_correct_user_and_system_deltas_from_pid_stat() {
        every { ProcFileReader.getCpuProcPidStatTime(PID) } returnsMany listOf(
            PID_STAT_SAMPLE_1,
            PID_STAT_SAMPLE_2
        )
        every { SystemClock.elapsedRealtime() } returnsMany listOf(0L, ELAPSED_MS)

        classToTest.getCpuInfoDto()  // first call
        val dto = classToTest.getCpuInfoDto()

        // userDelta = (utime2+cutime2) - (utime1+cutime1) = (150+25) - (100+20) = 55
        assertEquals(EXPECTED_USER_DELTA, dto.userTime, DELTA_TOLERANCE)
        // sysDelta = (stime2+cstime2) - (stime1+cstime1) = (75+15) - (50+10) = 30
        assertEquals(EXPECTED_SYS_DELTA, dto.systemTime, DELTA_TOLERANCE)
        assertEquals(0.0, dto.ioWaitTime, DELTA_TOLERANCE)
    }

    @Test
    fun getCpuInfoDto_returns_INVALID_when_pid_stat_read_fails() {
        every { ProcFileReader.getCpuProcPidStatTime(PID) } throws RuntimeException("read failed")

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
    }

    @Test
    fun getCpuInfoDto_returns_INVALID_when_pid_stat_line_has_too_few_fields() {
        every { ProcFileReader.getCpuProcPidStatTime(PID) } returns "123 (myapp) S 1 2"
        every { SystemClock.elapsedRealtime() } returns 0L

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
    }
}

private const val PID = 11916
private const val NUM_CORES = 4
private const val ELAPSED_MS = 1000L
private const val DELTA_TOLERANCE = 0.001

// utime=100, stime=50, cutime=20, cstime=10 → total=180
private const val PID_STAT_SAMPLE_1 =
    "$PID (com.smellouk.k+) S 296 296 0 0 -1 1077936448 1000 0 0 0 100 50 20 10 10 -10 32 0 123456"

// utime=150, stime=75, cutime=25, cstime=15 → total=265, delta=265-180=85
private const val PID_STAT_SAMPLE_2 =
    "$PID (com.smellouk.k+) S 296 296 0 0 -1 1077936448 1000 0 0 0 150 75 25 15 10 -10 32 0 123456"

// userDelta = (utime2+cutime2) - (utime1+cutime1) = (150+25) - (100+20) = 55
private const val EXPECTED_USER_DELTA = 55.0

// sysDelta = (stime2+cstime2) - (stime1+cstime1) = (75+15) - (50+10) = 30
private const val EXPECTED_SYS_DELTA = 30.0

// appDelta = userDelta + sysDelta = 55 + 30 = 85
private const val EXPECTED_APP_DELTA = 85.0

// totalTicks = (1000 / 10.0) * 4 = 400
private const val EXPECTED_TOTAL_TICKS = 400.0
