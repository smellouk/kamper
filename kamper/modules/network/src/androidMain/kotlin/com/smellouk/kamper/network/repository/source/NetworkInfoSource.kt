package com.smellouk.kamper.network.repository.source

import android.net.TrafficStats
import android.os.Process
import com.smellouk.kamper.network.repository.NetworkInfoDto

internal class NetworkInfoSource {
    // Visible only for testing
    internal lateinit var cachedDto: NetworkInfoDto
    fun getNetworkInfoDto(): NetworkInfoDto {
        val currentDto = NetworkInfoDto(
            rxTotalInBytes = TrafficStats.getTotalRxBytes(),
            txTotalInBytes = TrafficStats.getTotalTxBytes(),
            rxUidInBytes = TrafficStats.getUidRxBytes(Process.myUid()),
            txUidInBytes = TrafficStats.getUidTxBytes(Process.myUid())
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
}
