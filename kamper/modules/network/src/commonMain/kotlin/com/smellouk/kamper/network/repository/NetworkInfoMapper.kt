package com.smellouk.kamper.network.repository

import com.smellouk.kamper.api.bytesToMb
import com.smellouk.kamper.network.NetworkInfo

internal class NetworkInfoMapper {
    fun map(
        dto: NetworkInfoDto
    ): NetworkInfo = when {
        dto == NetworkInfoDto.INVALID -> {
            NetworkInfo.INVALID
        }
        dto == NetworkInfoDto.NOT_SUPPORTED -> {
            NetworkInfo.NOT_SUPPORTED
        }
        else -> {
            with(dto) {
                NetworkInfo(
                    rxSystemTotalInMb = rxTotalInBytes.bytesToMb(),
                    txSystemTotalInMb = txTotalInBytes.bytesToMb(),
                    rxAppInMb = rxUidInBytes.bytesToMb(),
                    txAppInMb = txUidInBytes.bytesToMb()
                )
            }
        }
    }
}
