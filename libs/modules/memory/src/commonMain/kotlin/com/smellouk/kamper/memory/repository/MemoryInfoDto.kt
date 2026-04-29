package com.smellouk.kamper.memory.repository

internal data class MemoryInfoDto(
    val maxMemoryInBytes: Long,
    val allocatedInBytes: Long,

    val totalPssInKiloBytes: Long?,
    val dalvikPssInKiloBytes: Long?,
    val nativePssInKiloBytes: Long?,
    val otherPssInKiloBytes: Long?,

    val availableRamInBytes: Long,
    val totalRamInBytes: Long,
    val lowRamThresholdInBytes: Long,
    val isLowMemory: Boolean
) {
    companion object {
        val INVALID = MemoryInfoDto(
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            false
        )
    }
}
