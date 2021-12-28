package com.smellouk.kamper.cpu.repository

data class CpuInfoDto(
    var user: Float,
    var system: Float,
    var idle: Float,
    var ioWait: Float,
    var total: Float,
    var app: Float
) {
    companion object {
        val INVALID = CpuInfoDto(
            -1F, -1F, -1F, -1F, -1F, -1F
        )
    }
}
