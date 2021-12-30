package com.smellouk.kamper.memory.repository

internal data class MemoryInfoDto(
    val freeMemoryInBytes: Long,
    val maxMemoryInBytes: Long,
    val allocatedInBytes: Long,

    val totalPssInBytes: Long?,
    val dalvikPssInBytes: Long?,
    val nativePssInBytes: Long?,
    val otherPssInBytes: Long?,

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
            -1,
            false
        )
    }
}
