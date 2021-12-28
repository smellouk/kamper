package com.smellouk.kamper.network.repository

import com.smellouk.kamper.api.bytesToMb
import com.smellouk.kamper.network.NetworkInfo

class NetworkInfoMapper {
    fun map(
        dto: NetworkInfoDto
    ): NetworkInfo = if (dto == NetworkInfoDto.INVALID) {
        NetworkInfo.INVALID
    } else {
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
