package com.smellouk.kamper.network.repository.source

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.network.repository.NetworkInfoDto
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.darwin.freeifaddrs
import platform.darwin.getifaddrs
import platform.darwin.ifaddrs
import platform.posix.AF_LINK

@OptIn(ExperimentalForeignApi::class)
internal class IosNetworkInfoSource(private val logger: Logger) {
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

    private fun readTotalBytes(): Pair<Long, Long>? =
        runCatching { readIfaddrs() }
            .onFailure { logger.log("Network read failed: ${it.message}") }
            .getOrNull()

    private fun readIfaddrs(): Pair<Long, Long>? = memScoped {
        val ifap = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(ifap.ptr) != 0) return@memScoped null

        val head = ifap.value ?: return@memScoped null
        var rx = 0L
        var tx = 0L

        var ifa: CPointer<ifaddrs>? = head
        while (ifa != null) {
            val name = ifa.pointed.ifa_name?.toKString()
            val addr = ifa.pointed.ifa_addr

            if (name != null && !name.startsWith("lo") &&
                addr != null && addr.pointed.sa_family.toInt() == AF_LINK
            ) {
                ifa.pointed.ifa_data?.let { rawData ->
                    // if_data struct not in iOS klib; read ifi_ibytes/ifi_obytes via
                    // raw word offsets (ABI-stable: offset 40/44 in struct if_data)
                    val words = rawData.reinterpret<UIntVar>()
                    rx += words[IFI_IBYTES_WORD_INDEX].toLong()
                    tx += words[IFI_OBYTES_WORD_INDEX].toLong()
                }
            }

            ifa = ifa.pointed.ifa_next
        }

        freeifaddrs(head)

        if (rx == 0L && tx == 0L) null else rx to tx
    }

    private companion object {
        // struct if_data: 8 u_char + 3 u_int32_t before counters.
        // ifi_ibytes at byte offset 40 → word index 10; ifi_obytes at 44 → word index 11.
        const val IFI_IBYTES_WORD_INDEX = 10
        const val IFI_OBYTES_WORD_INDEX = 11
    }
}
