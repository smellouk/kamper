package com.smellouk.kamper.network.repository.source

import android.net.TrafficStats
import android.os.Process
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.network.repository.NetworkInfoDto

internal class NetworkInfoSource(private val logger: Logger) {
    // Visible only for testing
    internal lateinit var cachedDto: NetworkInfoDto

    fun getNetworkInfoDto(): NetworkInfoDto {
        val rxTotal = TrafficStats.getTotalRxBytes()
        val txTotal = TrafficStats.getTotalTxBytes()
        val rxUid = TrafficStats.getUidRxBytes(Process.myUid())
        val txUid = TrafficStats.getUidTxBytes(Process.myUid())

        // System-level totals unsupported → device has no TrafficStats support at all
        if (rxTotal == UNSUPPORTED && txTotal == UNSUPPORTED) {
            logger.log("TrafficStats is not supported on this device")
            return NetworkInfoDto.NOT_SUPPORTED
        }

        // Per-UID tracking may be unsupported on API 21-22 devices; treat those as 0
        val currentDto = NetworkInfoDto(
            rxTotalInBytes = rxTotal.coerceAtLeast(0L),
            txTotalInBytes = txTotal.coerceAtLeast(0L),
            rxUidInBytes = rxUid.coerceAtLeast(0L),
            txUidInBytes = txUid.coerceAtLeast(0L)
        )

        return if (!this::cachedDto.isInitialized) {
            cachedDto = currentDto
            NetworkInfoDto.INVALID
        } else {
            NetworkInfoDto(
                rxTotalInBytes = currentDto.rxTotalInBytes - cachedDto.rxTotalInBytes,
                txTotalInBytes = currentDto.txTotalInBytes - cachedDto.txTotalInBytes,
                rxUidInBytes = currentDto.rxUidInBytes - cachedDto.rxUidInBytes,
                txUidInBytes = currentDto.txUidInBytes - cachedDto.txUidInBytes
            ).also {
                cachedDto = currentDto
            }
        }
    }

    private companion object {
        const val UNSUPPORTED = -1L
    }
}
