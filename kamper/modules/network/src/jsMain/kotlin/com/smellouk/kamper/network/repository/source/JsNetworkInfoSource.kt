package com.smellouk.kamper.network.repository.source

import com.smellouk.kamper.network.repository.NetworkInfoDto

internal class JsNetworkInfoSource {
    fun getNetworkInfoDto(): NetworkInfoDto {
        val downlinkMbps: Double = js("navigator.connection ? +navigator.connection.downlink : -1")
        if (downlinkMbps < 0) return NetworkInfoDto.NOT_SUPPORTED

        val rxBytesPerSec = (downlinkMbps * BITS_PER_MEGABIT / BITS_PER_BYTE).toLong()

        val uplinkMbps: Double = js("navigator.connection && navigator.connection.uplink ? +navigator.connection.uplink : 0")
        val txBytesPerSec = (uplinkMbps * BITS_PER_MEGABIT / BITS_PER_BYTE).toLong()

        return NetworkInfoDto(
            rxTotalInBytes = rxBytesPerSec,
            txTotalInBytes = txBytesPerSec,
            rxUidInBytes = 0L,
            txUidInBytes = 0L
        )
    }

    private companion object {
        const val BITS_PER_MEGABIT = 1_000_000.0
        const val BITS_PER_BYTE = 8.0
    }
}
