package com.smellouk.kamper.cpu.repository.source

import com.smellouk.kamper.cpu.repository.CpuInfoDto
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.getpid
import platform.posix.pclose
import platform.posix.popen

@OptIn(ExperimentalForeignApi::class)
internal class MacosCpuInfoSource : CpuInfoSource {
    override fun getCpuInfoDto(): CpuInfoDto {
        val topOutput = runCommand("top -l 1 -s 0 -n 0 2>/dev/null")
            ?: return CpuInfoDto.INVALID
        val cpuLine = topOutput.lineSequence().firstOrNull { it.startsWith("CPU usage:") }
            ?: return CpuInfoDto.INVALID

        // "CPU usage: X.XX% user, X.XX% sys, X.XX% idle"
        val userPct = parsePercent(cpuLine, "user") ?: return CpuInfoDto.INVALID
        val sysPct = parsePercent(cpuLine, "sys") ?: return CpuInfoDto.INVALID
        val idlePct = parsePercent(cpuLine, "idle") ?: return CpuInfoDto.INVALID

        val totalTime = 100.0
        val userTime = userPct * totalTime / 100.0
        val sysTime = sysPct * totalTime / 100.0
        val idleTime = idlePct * totalTime / 100.0
        val appTime = getProcessCpuPct()?.coerceIn(0.0, 100.0)?.times(totalTime / 100.0) ?: 0.0

        return CpuInfoDto(
            totalTime = totalTime,
            userTime = userTime,
            systemTime = sysTime,
            idleTime = idleTime,
            ioWaitTime = 0.0,
            appTime = appTime
        )
    }

    private fun getProcessCpuPct(): Double? {
        val pid = getpid()
        return runCommand("ps -p $pid -o pcpu= 2>/dev/null")?.trim()?.toDoubleOrNull()
    }

    private fun parsePercent(line: String, label: String): Double? {
        val regex = Regex("([\\d.]+)%\\s+$label")
        return regex.find(line)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun runCommand(cmd: String): String? = memScoped {
        val pipe = popen(cmd, "r") ?: return null
        val sb = StringBuilder()
        val buf = allocArray<ByteVar>(BUFFER_SIZE)
        while (fgets(buf, BUFFER_SIZE, pipe) != null) {
            sb.append(buf.toKString())
        }
        pclose(pipe)
        sb.toString()
    }

    private companion object {
        const val BUFFER_SIZE = 4096
    }
}
