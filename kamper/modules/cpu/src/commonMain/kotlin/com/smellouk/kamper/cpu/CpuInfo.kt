package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.Info

data class CpuInfo(
    val totalUseRatio: Double,
    val appRatio: Double,
    val userRatio: Double,
    val systemRatio: Double,
    val ioWaitRatio: Double
) : Info {
    companion object {
        val INVALID = CpuInfo(-1.0, -1.0, -1.0, -1.0, -1.0)
    }
}
