package com.smellouk.kamper.network.repository

data class NetworkInfoDto(
    val rxTotalInBytes: Long,
    val txTotalInBytes: Long,
    val rxUidInBytes: Long,
    val txUidInBytes: Long
) {
    companion object {
        val INVALID = NetworkInfoDto(
            -1,
            -1,
            -1,
            -1,
        )
    }
}
