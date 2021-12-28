package com.smellouk.kamper.cpu

import com.smellouk.kamper.api.Info

data class CpuInfo(
    val totalUseRatio: Float,
    val appRatio: Float,
    val userRatio: Float,
    val systemRatio: Float,
    val ioWaitRatio: Float
) : Info {
    companion object {
        val INVALID = CpuInfo(-1F, -1F, -1F, -1F, -1F)
    }
}
