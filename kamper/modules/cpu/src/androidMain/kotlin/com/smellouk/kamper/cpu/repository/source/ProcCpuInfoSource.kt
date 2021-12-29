package com.smellouk.kamper.cpu.repository.source

import android.os.Process
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import java.io.BufferedReader
import java.io.FileInputStream

/**
 * CPU parsing is based on this definition
 * https://man7.org/linux/man-pages/man5/proc.5.html
 */
internal class ProcCpuInfoSource(private val logger: Logger) : CpuInfoSource {
    private lateinit var cachedDto: CpuInfoDto

    override fun getCpuInfoDto(): CpuInfoDto {
        val pid = Process.myPid()
        val currentCpuInfoDto = parse(
            ProcFileReader.getCpuProcStatTime(),
            ProcFileReader.getCpuProcPidStatTime(pid)
        )

        return if (!this::cachedDto.isInitialized) {
            cachedDto = currentCpuInfoDto
            CpuInfoDto.INVALID
        } else {
            CpuInfoDto(
                totalTime = currentCpuInfoDto.totalTime - cachedDto.totalTime,
                idleTime = currentCpuInfoDto.idleTime - cachedDto.idleTime,
                appTime = currentCpuInfoDto.appTime - cachedDto.appTime,
                userTime = currentCpuInfoDto.userTime - cachedDto.userTime,
                systemTime = currentCpuInfoDto.systemTime - cachedDto.systemTime,
                ioWaitTime = currentCpuInfoDto.ioWaitTime - cachedDto.ioWaitTime
            ).also {
                cachedDto = currentCpuInfoDto
            }
        }
    }

    // Visible only for testing
    internal fun parse(
        cpuProcStatTime: String,
        cpuProcPidStatTime: String
    ): CpuInfoDto {
        try {
            val cpuProcStatTimeArray = cpuProcStatTime.split("\\s+".toRegex())
            if (cpuProcStatTimeArray.size < EXPECTED_CPU_INFO_SIZE) {
                logger.log(
                    "Cpu info list size[${cpuProcStatTimeArray.size}] " +
                            "must be >= $EXPECTED_CPU_INFO_SIZE"
                )
                return CpuInfoDto.INVALID
            }
            val user = cpuProcStatTimeArray[CPU_USER_INDEX].toDouble()
            val nice = cpuProcStatTimeArray[CPU_NICE_INDEX].toDouble()
            val system = cpuProcStatTimeArray[CPU_SYSTEM_INDEX].toDouble()
            val idle = cpuProcStatTimeArray[CPU_IDLE_INDEX].toDouble()
            val ioWait = cpuProcStatTimeArray[CPU_IO_WAIT_INDEX].toDouble()
            val irq = cpuProcStatTimeArray[CPU_IRQ_INDEX].toDouble()
            val softIrq = cpuProcStatTimeArray[CPU_IRQ_SOFT_IRQ_INDEX].toDouble()
            val steal = cpuProcStatTimeArray[CPU_STEAL_INDEX].toDouble()
            val total = user + nice + system + idle + ioWait + irq + softIrq + steal

            val cpuProcPidStatTimeArray = cpuProcPidStatTime.split(" ")
            if (cpuProcPidStatTimeArray.size < EXPECTED_CPU_PID_INFO_SIZE) {
                logger.log(
                    "Pid cpu info list size[${cpuProcPidStatTimeArray.size}] " +
                            "must be >= $EXPECTED_CPU_PID_INFO_SIZE"
                )
                return CpuInfoDto.INVALID
            }
            val uTime = cpuProcPidStatTimeArray[CPU_PID_U_TIME_INDEX].toDouble()
            val sTime = cpuProcPidStatTimeArray[CPU_PID_S_TIME_INDEX].toDouble()
            val cuTime = cpuProcPidStatTimeArray[CPU_PID_CU_TIME_INDEX].toDouble()
            val csTime = cpuProcPidStatTimeArray[CPU_PID_CS_TIME_INDEX].toDouble()
            val appTime = uTime + sTime + cuTime + csTime

            return CpuInfoDto(
                totalTime = total,
                userTime = user,
                systemTime = system,
                idleTime = idle,
                ioWaitTime = ioWait,
                appTime = appTime
            )
        } catch (throwable: Throwable) {
            logger.log(throwable.stackTraceToString())
        }

        return CpuInfoDto.INVALID
    }
}

internal object ProcFileReader {
    // Visible only for testing
    internal fun getCpuProcStatTime(): String = FileInputStream("/proc/stat")
        .bufferedReader()
        .use(BufferedReader::readLine)

    // Visible only for testing
    internal fun getCpuProcPidStatTime(pid: Int): String = FileInputStream("/proc/$pid/stat")
        .bufferedReader()
        .use(BufferedReader::readLine)
}

private const val EXPECTED_CPU_INFO_SIZE = 9
private const val CPU_USER_INDEX = 1
private const val CPU_NICE_INDEX = 2
private const val CPU_SYSTEM_INDEX = 3
private const val CPU_IDLE_INDEX = 4
private const val CPU_IO_WAIT_INDEX = 5
private const val CPU_IRQ_INDEX = 6
private const val CPU_IRQ_SOFT_IRQ_INDEX = 7
private const val CPU_STEAL_INDEX = 8

private const val EXPECTED_CPU_PID_INFO_SIZE = 17
private const val CPU_PID_U_TIME_INDEX = 13
private const val CPU_PID_S_TIME_INDEX = 14
private const val CPU_PID_CU_TIME_INDEX = 15
private const val CPU_PID_CS_TIME_INDEX = 16
