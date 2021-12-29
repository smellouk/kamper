package com.smellouk.kamper.cpu.repository.source

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShellCpuInfoSourceTest {
    private val logger = mockk<Logger>(relaxed = true)

    private val classToTest: ShellCpuInfoSource = ShellCpuInfoSource(logger)

    @Before
    fun setup() {
        mockkStatic(Runtime::class)
        mockkStatic(android.os.Process::class)
        every { android.os.Process.myPid() } returns PID
    }

    @After
    fun tearDown() {
        unmockkStatic(Runtime::class)
        unmockkStatic(android.os.Process::class)
    }

    @Test
    fun `readAllLines should read all lines from InputStream`() {
        val anyInputStream = ByteArrayInputStream(TOP_N_1_CMD_OUTPUT.toByteArray())

        val lines = anyInputStream.readAllLine()

        assertEquals(EXPECTED_LINES, lines)
    }

    @Test
    fun `isCpuInfoUsageLine should return true when line is CPU_USAGE_LINE`() {
        assertTrue(CPU_USAGE_LINE.isCpuInfoUsageLine())
    }

    @Test
    fun `isCpuInfoUsageLine should return false when line is not CPU_USAGE_LINE`() {
        assertFalse(PROCESS_LABELS_LINE.isCpuInfoUsageLine())
    }

    @Test
    fun `toCpuInfoUsageMap should create correct cpu usage map`() {
        val map = CPU_USAGE_LINE.toCpuInfoUsageMap()

        assertEquals(EXPECTED_CPU_USAGE_MAP, map)
    }

    @Test
    fun `toCpuInfoUsageMap should create empty map when line is not parsable`() {
        val map = TOP_N_1_CMD_OUTPUT.toCpuInfoUsageMap()

        assertEquals(emptyMap(), map)
    }

    @Test
    fun `isProcessLabelLine should return true when line is PROCESS_LABELS_LINE`() {
        assertTrue(PROCESS_LABELS_LINE.isProcessLabelLine())
    }

    @Test
    fun `isProcessLabelLine should return false when line is not PROCESS_LABELS_LINE`() {
        assertFalse(CPU_USAGE_LINE.isProcessLabelLine())
    }

    @Test
    fun `getCpuLabelIndex should return CPU_INDEX`() {
        val index = PROCESS_LABELS_LINE.getCpuLabelIndex()

        assertEquals(EXPECTED_CPU_INDEX, index)
    }

    @Test
    fun `isProcessAppDetailsLine should return true when line start with PID`() {
        assertTrue(APP_PROCESS_DETAILS_LINE.isProcessAppDetailsLine(PID))
    }

    @Test
    fun `isProcessAppDetailsLine should return false when line does not start with PID`() {
        assertFalse(PROCESS_LABELS_LINE.isProcessAppDetailsLine(PID))
    }

    @Test
    fun `getCpuInfoDto should return invalid cpu info when cmdOutputLines is empty`() {
        val process = mockRuntimeProcess()

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
        verify { process.inputStream }
        verify { process.destroy() }
        confirmVerified(process)
    }

    @Test
    fun `getCpuInfoDto should return invalid cpu info when cmdOutputLines is any cmd output`() {
        val process = mockRuntimeProcess("ANY_COMMAND_OUTPUT")

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
        verify { process.inputStream }
        verify { process.destroy() }
        confirmVerified(process)
    }

    @Test
    fun `getCpuInfoDto should return valid cpu info when cmdOutputLines is valid output`() {
        val process = mockRuntimeProcess(TOP_N_1_CMD_OUTPUT)

        val dto = classToTest.getCpuInfoDto()

        assertEquals(EXPECTED_VALID_DTO, dto)
        verify { Runtime.getRuntime().exec("top -n 1") }
        verify { android.os.Process.myPid() }
        verify { process.inputStream }
        verify { process.destroy() }
        confirmVerified(process)
    }

    @Test
    fun `getCpuInfoDto should return invalid cpu info when exception is thrown`() {
        every { Runtime.getRuntime().exec("top -n 1") } throws Exception("ANY_EXCEPTION")

        val dto = classToTest.getCpuInfoDto()

        assertEquals(CpuInfoDto.INVALID, dto)
    }

    private fun mockRuntimeProcess(
        cmdLineOutput: String = ""
    ) = mockk<Process>(relaxed = true).apply {
        every { inputStream } returns ByteArrayInputStream(cmdLineOutput.toByteArray())
        every { destroy() } returns Unit
    }.also { process ->
        every { Runtime.getRuntime().exec("top -n 1") } returns process
    }
}

private const val CPU_USAGE_LINE =
    "400%cpu  11%user   0%nice  11%sys 379%idle   0%iow   0%irq   0%sirq   0%host"

private const val PROCESS_LABELS_LINE =
    "\u001B[7m   PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS           \u001B[0m"

private const val PID = 11916
private const val APP_PROCESS_DETAILS_LINE =
    "$PID u0_a149      10 -10  13G 149M  90M S 40.0   7.5   0:02.61 com.smellouk.k+"

private const val TOP_N_1_CMD_OUTPUT = """
[s[999C[999B[6n[u[H[J[?25l[H[J[s[999C[999B[6n[uTasks: 2 total,   1 running,   1 sleeping,   0 stopped,   0 zombie
  Mem:  2015364K total,  1776196K used,   239168K free,    20132K buffers
 Swap:  1511516K total,   355328K used,  1156188K free,   800780K cached
400%cpu  11%user   0%nice  11%sys 379%idle   0%iow   0%irq   0%sirq   0%host
[7m   PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS           [0m
 11916 u0_a149      10 -10  13G 149M  90M S 40.0   7.5   0:02.61 com.smellouk.k+
[1m 11951 u0_a149      20   0  10G 3.4M 2.7M R  0.0   0.1   0:00.02 top -n 1
[m[?25h[0m[1000;1H[K[?25h[?25h[0m[1000;1H[K
"""

private val EXPECTED_LINES = listOf(
    "\u001B[s\u001B[999C\u001B[999B\u001B[6n\u001B[u\u001B[H\u001B[J\u001B[?25l\u001B[H\u001B[J\u001B[s\u001B[999C\u001B[999B\u001B[6n\u001B[uTasks: 2 total,   1 running,   1 sleeping,   0 stopped,   0 zombie",
    "Mem:  2015364K total,  1776196K used,   239168K free,    20132K buffers",
    "Swap:  1511516K total,   355328K used,  1156188K free,   800780K cached",
    CPU_USAGE_LINE,
    PROCESS_LABELS_LINE,
    APP_PROCESS_DETAILS_LINE,
    "\u001B[1m 11951 u0_a149      20   0  10G 3.4M 2.7M R  0.0   0.1   0:00.02 top -n 1",
    "\u001B[m\u001B[?25h\u001B[0m\u001B[1000;1H\u001B[K\u001B[?25h\u001B[?25h\u001B[0m\u001B[1000;1H\u001B[K"
)

private val EXPECTED_CPU_USAGE_MAP = mapOf(
    "cpu" to 400.0,
    "user" to 11.0,
    "nice" to 0.0,
    "sys" to 11.0,
    "idle" to 379.0,
    "iow" to 0.0,
    "irq" to 0.0,
    "sirq" to 0.0,
    "host" to 0.0
)

private const val EXPECTED_CPU_INDEX = 8

private val EXPECTED_VALID_DTO = CpuInfoDto(
    totalTime = 400.0,
    userTime = 11.0,
    systemTime = 11.0,
    idleTime = 379.0,
    ioWaitTime = 0.0,
    appTime = 40.0
)
