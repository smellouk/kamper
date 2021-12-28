package com.smellouk.kamper.cpu.repository.source

import android.os.Process
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import java.io.BufferedReader
import java.io.FileInputStream

internal class ProcCpuInfoSource : CpuInfoSource {
    private lateinit var cachedDto: CpuInfoDto

    override fun getCpuInfoDto(): CpuInfoDto {
        val pid = Process.myPid()
        val currentCpuInfoRaw = parse(getCpuRateOfDevice(), getCpuRateOfApp(pid))

        return if (!this::cachedDto.isInitialized) {
            cachedDto = currentCpuInfoRaw
            CpuInfoDto.INVALID
        } else {
            CpuInfoDto(
                total = currentCpuInfoRaw.total - cachedDto.total,
                idle = currentCpuInfoRaw.idle - cachedDto.idle,
                app = currentCpuInfoRaw.app - cachedDto.app,
                user = currentCpuInfoRaw.user - cachedDto.user,
                system = currentCpuInfoRaw.system - cachedDto.system,
                ioWait = currentCpuInfoRaw.ioWait - cachedDto.ioWait
            )
        }
    }

    private fun getCpuRateOfDevice(): String = FileInputStream("/proc/stat")
        .bufferedReader()
        .use(BufferedReader::readLine)

    private fun getCpuRateOfApp(pid: Int): String = FileInputStream("/proc/$pid/stat")
        .bufferedReader()
        .use(BufferedReader::readLine)

    @Suppress("MagicNumber")
    private fun parse(
        cpuRate: String,
        pidCpuRate: String
    ): CpuInfoDto {
        val cpuInfoList = cpuRate.split("\\s+".toRegex())
        check(cpuInfoList.size >= 9) { "Cpu info list size must be >= 9" }

        val user = cpuInfoList[2].toFloat()
        val nice = cpuInfoList[3].toFloat()
        val system = cpuInfoList[4].toFloat()
        val idle = cpuInfoList[5].toFloat()
        val ioWait = cpuInfoList[6].toFloat()
        val total = user + nice + system + idle + ioWait
        +cpuInfoList[7].toFloat() + cpuInfoList[8].toFloat()
        val pidCpuInfoList = pidCpuRate.split(" ")
        check(pidCpuInfoList.size >= 17) { "Pid cpu info list size must be >= 17" }

        val appCpuTime = pidCpuInfoList[13].toFloat() + pidCpuInfoList[14].toFloat() +
                pidCpuInfoList[15].toFloat() + pidCpuInfoList[16].toFloat()

        return CpuInfoDto(
            user,
            system,
            idle,
            ioWait,
            total,
            appCpuTime
        )
    }
}
