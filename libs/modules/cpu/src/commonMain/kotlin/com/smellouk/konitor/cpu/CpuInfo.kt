package com.smellouk.konitor.cpu

import com.smellouk.konitor.api.Info

data class CpuInfo(
    val totalUseRatio: Double,
    val appRatio: Double,
    val userRatio: Double,
    val systemRatio: Double,
    val ioWaitRatio: Double
) : Info {
    companion object {
        val INVALID = CpuInfo(-1.0, -1.0, -1.0, -1.0, -1.0)
        val UNSUPPORTED = CpuInfo(-2.0, -2.0, -2.0, -2.0, -2.0)
    }
}
