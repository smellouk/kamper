package com.smellouk.konitor.network.repository.source

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.network.repository.NetworkInfoDto
import java.io.File

internal class JvmNetworkInfoSource(private val logger: Logger) {
    private var cachedDto: NetworkInfoDto? = null

    fun getNetworkInfoDto(): NetworkInfoDto {
        val current = readTotalBytes() ?: return NetworkInfoDto.NOT_SUPPORTED

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
        val osName = System.getProperty("os.name").lowercase()
        when {
            File("/proc/net/dev").exists() -> readLinux()
            osName.contains("mac") -> readMacOs()
            else -> null
        }
    }.onFailure { logger.log("Failed to read network stats: ${it.message}") }.getOrNull()

    private fun readLinux(): Pair<Long, Long> {
        var rx = 0L
        var tx = 0L
        File("/proc/net/dev").readLines().drop(2).forEach { line ->
            val parts = line.trim().split("\\s+".toRegex())
            val iface = parts.getOrNull(0)?.trimEnd(':') ?: return@forEach
            if (iface == "lo" || parts.size < 10) return@forEach
            rx += parts[1].toLongOrNull() ?: 0L
            tx += parts[9].toLongOrNull() ?: 0L
        }
        return rx to tx
    }

    private fun readMacOs(): Pair<Long, Long>? {
        val output = Runtime.getRuntime()
            .exec(arrayOf("netstat", "-ib"))
            .inputStream.bufferedReader().readText()

        var rx = 0L
        var tx = 0L
        // Only count <Link#> rows (hardware level) for non-loopback interfaces
        output.lines().drop(1).forEach { line ->
            val parts = line.trim().split("\\s+".toRegex())
            val name = parts.getOrNull(0) ?: return@forEach
            val network = parts.getOrNull(2) ?: return@forEach
            if (name.startsWith("lo") || !network.startsWith("<Link#")) return@forEach

            // Address field present → indices shift by 1 (Ibytes at 6, Obytes at 9)
            // Address field absent  → Ibytes at 5, Obytes at 8
            val hasAddress = parts.getOrNull(3)?.contains(':') == true
            val ibytesIdx = if (hasAddress) 6 else 5
            val obytesIdx = if (hasAddress) 9 else 8
            rx += parts.getOrNull(ibytesIdx)?.toLongOrNull() ?: 0L
            tx += parts.getOrNull(obytesIdx)?.toLongOrNull() ?: 0L
        }
        return if (rx == 0L && tx == 0L) null else rx to tx
    }
}
