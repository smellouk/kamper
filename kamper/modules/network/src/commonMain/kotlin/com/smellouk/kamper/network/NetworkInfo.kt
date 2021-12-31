package com.smellouk.kamper.network

import com.smellouk.kamper.api.Info

data class NetworkInfo(
    val rxSystemTotalInMb: Float,
    val txSystemTotalInMb: Float,
    val rxAppInMb: Float,
    val txAppInMb: Float
) : Info {
    companion object {
        val INVALID = NetworkInfo(
            -1F, -1F, -1F, -1F
        )
        val NOT_SUPPORTED = NetworkInfo(
            -100F, -100F, -100F, -100F
        )
    }
}
