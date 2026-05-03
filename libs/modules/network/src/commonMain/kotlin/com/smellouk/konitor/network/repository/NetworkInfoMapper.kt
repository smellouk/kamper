package com.smellouk.konitor.network.repository

import com.smellouk.konitor.api.bytesToMb
import com.smellouk.konitor.network.NetworkInfo

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
