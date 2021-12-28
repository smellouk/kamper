package com.smellouk.kamper.network.repository

import com.smellouk.kamper.api.toMb
import com.smellouk.kamper.network.NetworkInfo

class NetworkInfoMapper {
    fun map(
        dto: NetworkInfoDto
    ): NetworkInfo = if (dto == NetworkInfoDto.INVALID) {
        NetworkInfo.INVALID
    } else {
        with(dto) {
            NetworkInfo(
                rxSystemTotalInMb = rxTotalInBytes.toMb(),
                txSystemTotalInMb = txTotalInBytes.toMb(),
                rxAppInMb = rxUidInBytes.toMb(),
                txAppInMb = txUidInBytes.toMb()
            )
        }
    }
}
