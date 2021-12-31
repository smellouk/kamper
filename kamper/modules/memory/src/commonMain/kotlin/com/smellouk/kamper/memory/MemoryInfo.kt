package com.smellouk.kamper.memory

import com.smellouk.kamper.api.Info

data class MemoryInfo(
    val heapMemoryInfo: HeapMemoryInfo,
    val pssInfo: PssInfo,
    val ramInfo: RamInfo
) : Info {
    companion object {
        val INVALID = MemoryInfo(
            HeapMemoryInfo.INVALID, PssInfo.INVALID, RamInfo.INVALID
        )
    }

    data class HeapMemoryInfo(
        val maxMemoryInMb: Float,
        val allocatedInMb: Float
    ) {
        companion object {
            val INVALID = HeapMemoryInfo(
                -1F, -1F
            )
        }
    }

    data class PssInfo(
        val totalPssInMb: Float,
        val dalvikPssInMb: Float,
        val nativePssInMb: Float,
        val otherPssInMb: Float
    ) {
        companion object {
            val INVALID = PssInfo(
                -1F, -1F, -1F, -1F
            )
        }
    }

    data class RamInfo(
        val availableRamInMb: Float,
        val totalRamInMb: Float,
        val lowRamThresholdInMb: Float,
        val isLowMemory: Boolean
    ) {
        companion object {
            val INVALID = RamInfo(
                -1F, -1F, -1F, false
            )
        }
    }
}
