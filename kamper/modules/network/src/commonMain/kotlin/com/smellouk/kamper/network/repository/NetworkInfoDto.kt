package com.smellouk.kamper.network.repository

internal data class NetworkInfoDto(
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
        val NOT_SUPPORTED = NetworkInfoDto(
            -100,
            -100,
            -100,
            -100,
        )
    }
}
