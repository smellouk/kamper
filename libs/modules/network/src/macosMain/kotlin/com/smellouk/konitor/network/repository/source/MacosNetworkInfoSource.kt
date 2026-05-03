package com.smellouk.konitor.network.repository.source

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.network.repository.NetworkInfoDto
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

@OptIn(ExperimentalForeignApi::class)
internal class MacosNetworkInfoSource(private val logger: Logger) {
    private var cachedDto: NetworkInfoDto? = null

    fun getNetworkInfoDto(): NetworkInfoDto {
        val current = readTotalBytes()
            ?: return NetworkInfoDto.NOT_SUPPORTED

        val cached = cachedDto
        val currentDto = NetworkInfoDto(
            rxTotalInBytes = current.first,
            txTotalInBytes = current.second,
            rxUidInBytes = 0L,
            txUidInBytes = 0L
        )
        cachedDto = currentDto

        return if (cached == null) {
            NetworkInfoDto.INVALID
        } else {
            NetworkInfoDto(
                rxTotalInBytes = maxOf(0L, current.first - cached.rxTotalInBytes),
                txTotalInBytes = maxOf(0L, current.second - cached.txTotalInBytes),
                rxUidInBytes = 0L,
                txUidInBytes = 0L
            )
        }
    }

    private fun readTotalBytes(): Pair<Long, Long>? = runCatching {
        readNetstat()
    }.onFailure { logger.log("Failed to read network stats: ${it.message}") }.getOrNull()

    private fun readNetstat(): Pair<Long, Long>? {
        val output = runCommand("netstat -ib 2>/dev/null") ?: return null
        var rx = 0L
        var tx = 0L
        // Only count <Link#> rows for non-loopback interfaces
        output.lineSequence().drop(1).forEach { line ->
            val parts = line.trim().split("\\s+".toRegex())
            val name = parts.getOrNull(0) ?: return@forEach
            val network = parts.getOrNull(2) ?: return@forEach
            if (name.startsWith("lo") || !network.startsWith("<Link#")) return@forEach

            // Address field present (contains ':') → Ibytes at index 6, Obytes at index 9
            // Address field absent               → Ibytes at index 5, Obytes at index 8
            val hasAddress = parts.getOrNull(3)?.contains(':') == true
            val ibytesIdx = if (hasAddress) 6 else 5
            val obytesIdx = if (hasAddress) 9 else 8
            rx += parts.getOrNull(ibytesIdx)?.toLongOrNull() ?: 0L
            tx += parts.getOrNull(obytesIdx)?.toLongOrNull() ?: 0L
        }
        return if (rx == 0L && tx == 0L) null else rx to tx
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
        const val BUFFER_SIZE = 8192
    }
}
