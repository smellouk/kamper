package com.smellouk.konitor.memory.repository.source

import com.smellouk.konitor.memory.repository.MemoryInfoDto
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
internal class MacosMemoryInfoSource {
    fun getMemoryInfoDto(): MemoryInfoDto {
        val totalRam = readTotalRam()
        val availRam = readAvailableRam()
        val processRss = readProcessRss()
        val threshold = totalRam / LOW_MEMORY_THRESHOLD_DIVISOR
        val isLowMemory = availRam > 0L && availRam < threshold

        return MemoryInfoDto(
            maxMemoryInBytes = totalRam / PROCESS_MAX_RATIO,
            allocatedInBytes = processRss,
            totalPssInKiloBytes = null,
            dalvikPssInKiloBytes = null,
            nativePssInKiloBytes = null,
            otherPssInKiloBytes = null,
            availableRamInBytes = availRam,
            totalRamInBytes = totalRam,
            lowRamThresholdInBytes = threshold,
            isLowMemory = isLowMemory
        )
    }

    private fun readTotalRam(): Long {
        val output = runCommand("sysctl -n hw.memsize 2>/dev/null")?.trim() ?: return 0L
        return output.toLongOrNull() ?: 0L
    }

    private fun readAvailableRam(): Long {
        val vmStat = runCommand("vm_stat 2>/dev/null") ?: return 0L
        val freePages = vmStat.lineSequence()
            .firstOrNull { it.contains("Pages free:") }
            ?.substringAfter("Pages free:")
            ?.trim()
            ?.trimEnd('.')
            ?.toLongOrNull() ?: 0L
        val inactivePages = vmStat.lineSequence()
            .firstOrNull { it.contains("Pages inactive:") }
            ?.substringAfter("Pages inactive:")
            ?.trim()
            ?.trimEnd('.')
            ?.toLongOrNull() ?: 0L
        return (freePages + inactivePages) * PAGE_SIZE_BYTES
    }

    private fun readProcessRss(): Long {
        val pid = getpid()
        val output = runCommand("ps -p $pid -o rss= 2>/dev/null")?.trim() ?: return 0L
        return (output.toLongOrNull() ?: 0L) * KILOBYTES_TO_BYTES
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
        const val LOW_MEMORY_THRESHOLD_DIVISOR = 20L
        const val PAGE_SIZE_BYTES = 4096L
        const val KILOBYTES_TO_BYTES = 1024L
        const val PROCESS_MAX_RATIO = 4L
        const val BUFFER_SIZE = 4096
    }
}
