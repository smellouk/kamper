package com.smellouk.kamper.cpu.repository

internal data class CpuInfoDto(
    var totalTime: Double,
    var userTime: Double,
    var systemTime: Double,
    var idleTime: Double,
    var ioWaitTime: Double,
    var appTime: Double
) {
    companion object {
        val INVALID = CpuInfoDto(
            -1.0, -1.0, -1.0, -1.0, -1.0, -1.0
        )
    }
}
