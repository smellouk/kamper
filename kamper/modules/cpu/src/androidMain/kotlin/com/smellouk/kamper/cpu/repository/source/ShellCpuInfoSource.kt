package com.smellouk.kamper.cpu.repository.source

import android.os.Build
import androidx.annotation.RequiresApi
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import java.io.InputStream
import android.os.Process as ProcessPidProvider

/**
 * Complicate logic for parsing CPU usage on android +Oreo, Check unit tests to see how the
 * logic should work.
 */
internal class ShellCpuInfoSource(
    private val logger: Logger
) : CpuInfoSource {

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun getCpuInfoDto(): CpuInfoDto {
        var process: Process? = null
        val pid = ProcessPidProvider.myPid()
        try {
            process = Runtime.getRuntime().exec("top -n 1")
            val cmdOutputLines = process?.inputStream?.readAllLine() ?: emptyList()
            if (cmdOutputLines.isEmpty()) {
                return CpuInfoDto.INVALID
            }

            var cpuInfoUsageMap = emptyMap<String, Double>()
            var cpuLabelIndex = -1
            var cpuAppUsage = -1.0
            cmdOutputLines.forEach { line ->
                // 400%cpu   0%user   0%nice   0%sys 400%idle   0%iow   0%irq   0%sirq   0%host
                if (line.isCpuInfoUsageLine()) {
                    cpuInfoUsageMap = line.toCpuInfoUsageMap()
                }

                // [7m   PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS           [0m
                if (line.isProcessLabelLine()) {
                    cpuLabelIndex = line.getCpuLabelIndex()
                }

                // 10062 u0_a149      10 -10  13G 150M  91M S  0.0   7.6   0:09.79 com.smellouk.k+
                if (line.isProcessAppDetailsLine(pid) && cpuLabelIndex != -1) {
                    cpuAppUsage = line.getCpuAppUsage(cpuLabelIndex)
                }
            }

            if (cpuInfoUsageMap.isEmpty()) {
                return CpuInfoDto.INVALID
            }

            val total = cpuInfoUsageMap["cpu"]
            val user = cpuInfoUsageMap["user"]
            val sys = cpuInfoUsageMap["sys"]
            val idle = cpuInfoUsageMap["idle"]
            val iow = cpuInfoUsageMap["iow"]

            return CpuInfoDto(
                totalTime = total.normalize(),
                userTime = user.normalize(),
                systemTime = sys.normalize(),
                idleTime = idle.normalize(),
                ioWaitTime = iow.normalize(),
                appTime = cpuAppUsage.normalize()
            )
        } catch (e: Exception) {
            logger.log(e.stackTraceToString())
        } finally {
            process?.destroy()
        }

        return CpuInfoDto.INVALID
    }
}

// Visible only for testing
internal fun InputStream.readAllLine(): List<String> =
    bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.filter { line ->
            line.isNotBlank()
        }.map { line ->
            line.trim()
        }.toList()
    }

// Visible only for testing
internal fun String.isCpuInfoUsageLine(): Boolean = matches("^\\d+%\\w+.+\\d+%\\w+".toRegex())

// Visible only for testing
internal fun String.toCpuInfoUsageMap(): Map<String, Double> = try {
    lowercase()
        .split("\\s+".toRegex())
        .map { cpuRaw -> cpuRaw.split("%") }
        .filter { cpuItem -> cpuItem.size >= 2 }
        .map { cpuItem -> cpuItem[1] to cpuItem[0].toDouble() }
        .toMap()
} catch (ignore: Throwable) {
    emptyMap()
}

// Visible only for testing
internal fun String.isProcessLabelLine(): Boolean = contains("PID USER")

// Visible only for testing
internal fun String.getCpuLabelIndex(): Int = this.split("\\s+".toRegex())
    .indexOfFirst { element -> element.contains("CPU") }

// Visible only for testing
internal fun String.isProcessAppDetailsLine(pid: Int): Boolean =
    startsWith(pid.toString())

// Visible only for testing
internal fun String.getCpuAppUsage(cpuIndex: Int): Double {
    return split("\\s+".toRegex()).takeIf { params ->
        params.isNotEmpty() && params.size >= cpuIndex
    }?.elementAt(cpuIndex)?.toDouble() ?: -1.0
}

// Visible only for testing
internal fun Double?.normalize(): Double = if (this == null || this < -1F) {
    0.0
} else {
    this
}
