package com.smellouk.kamper.network.repository.source

import com.smellouk.kamper.network.repository.NetworkInfoDto

@JsFun("() => navigator.connection ? +navigator.connection.downlink : -1")
private external fun jsGetDownlinkMbps(): Double

@JsFun("() => (navigator.connection && navigator.connection.uplink) ? +navigator.connection.uplink : 0")
private external fun jsGetUplinkMbps(): Double

internal class JsNetworkInfoSource {
    fun getNetworkInfoDto(): NetworkInfoDto {
        val downlinkMbps = jsGetDownlinkMbps()
        if (downlinkMbps < 0) return NetworkInfoDto.NOT_SUPPORTED

        val rxBytesPerSec = (downlinkMbps * BITS_PER_MEGABIT / BITS_PER_BYTE).toLong()
        val txBytesPerSec = (jsGetUplinkMbps() * BITS_PER_MEGABIT / BITS_PER_BYTE).toLong()

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
