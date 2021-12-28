package com.smellouk.kamper.cpu.repository.source

import android.os.Build
import androidx.annotation.RequiresApi
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import java.io.InputStream
import java.util.Locale

internal class ShellCpuInfoSource(
    private val logger: Logger
) : CpuInfoSource {

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun getCpuInfoDto(): CpuInfoDto {
        var process: Process? = null
        try {
            process = Runtime.getRuntime().exec("top -n 1")
            val cmdOutputLines = process?.inputStream?.readAllLine() ?: emptyList()
            if (cmdOutputLines.isEmpty()) {
                return CpuInfoDto.INVALID
            }

            var cpuTasksMap = emptyMap<String, Float>()
            var cpuLabelIndex = -1
            var cpuAppUsage = -1F
            cmdOutputLines.forEach { line ->
                if (line.isCpuTasksLine()) {
                    cpuTasksMap = line.toCpuTasksMap()
                }
                if (line.isCpuLabelLine()) {
                    cpuLabelIndex = line.toCPULabelIndex()
                }
                if (line.isCpuAppUsageLine()) {
                    cpuAppUsage = line.getCpuAppUsage(cpuLabelIndex)
                }
            }

            if (cpuTasksMap.isEmpty()) {
                return CpuInfoDto.INVALID
            }

            val total = cpuTasksMap["cpu"]
            val user = cpuTasksMap["user"]
            val sys = cpuTasksMap["sys"]
            val idle = cpuTasksMap["idle"]
            val iow = cpuTasksMap["iow"]

            return CpuInfoDto(
                total = total.normalize(),
                user = user.normalize(),
                system = sys.normalize(),
                idle = idle.normalize(),
                ioWait = iow.normalize(),
                app = cpuAppUsage.normalize()
            )
        } catch (e: Exception) {
            logger.log(e.stackTraceToString())
        } finally {
            process?.destroy()
        }

        return CpuInfoDto.INVALID
    }

    private fun InputStream.readAllLine(): List<String> =
        bufferedReader(Charsets.ISO_8859_1).useLines { lines ->
            lines.filter { line ->
                line.isNotBlank()
            }.map { line ->
                line.trim()
            }
                .toList()
        }

    private fun String.isCpuTasksLine(): Boolean = matches("^\\d+%\\w+.+\\d+%\\w+".toRegex())

    private fun String.toCpuTasksMap(): Map<String, Float> = lowercase(Locale.US)
        .split("\\s+".toRegex())
        .map { cpuRaw -> cpuRaw.split("%") }
        .filter { cpuItem -> cpuItem.size >= 2 }
        .map { cpuItem -> cpuItem[1] to cpuItem[0].toFloat() }
        .toMap()

    private fun String.isCpuLabelLine(): Boolean = contains("CPU")

    private fun String.toCPULabelIndex(): Int = this.split("\\s+".toRegex())
        .indexOfFirst { element -> element.contains("CPU") }

    private fun String.isCpuAppUsageLine(): Boolean =
        startsWith(android.os.Process.myPid().toString())

    private fun String.getCpuAppUsage(cpuIndex: Int): Float {
        return split("\\s+".toRegex()).takeIf { params ->
            params.isNotEmpty() && params.size >= cpuIndex
        }?.elementAt(cpuIndex)?.toFloat() ?: -1F
    }

    private fun Float?.normalize(): Float = if (this == null || this < -1F) {
        0F
    } else {
        this
    }
}
