package com.smellouk.kamper.memory.repository

import com.smellouk.kamper.api.bytesToMb
import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.MemoryInfo.AppMemoryInfo
import com.smellouk.kamper.memory.MemoryInfo.PssInfo
import com.smellouk.kamper.memory.MemoryInfo.RamInfo

class MemoryInfoMapper {
    fun map(dto: MemoryInfoDto): MemoryInfo = if (dto == MemoryInfoDto.INVALID) {
        MemoryInfo.INVALID
    } else {
        with(dto) {
            MemoryInfo(
                appMemoryInfo = AppMemoryInfo(
                    freeMemoryInMb = freeMemoryInBytes.bytesToMb(),
                    maxMemoryInMb = maxMemoryInBytes.bytesToMb(),
                    allocatedInMb = allocatedInBytes.bytesToMb(),
                ),
                pssInfo = PssInfo(
                    totalPssInMb = totalPssInBytes?.bytesToMb() ?: -1F,
                    dalvikPssInMb = dalvikPssInBytes?.bytesToMb() ?: -1F,
                    nativePssInMb = nativePssInBytes?.bytesToMb() ?: -1F,
                    otherPssInMb = otherPssInBytes?.bytesToMb() ?: -1F
                ),
                ramInfo = RamInfo(
                    availableRamInMb = availableRamInBytes.bytesToMb(),
                    totalRamInMb = totalRamInBytes.bytesToMb(),
                    lowRamThresholdInMb = lowRamThresholdInBytes.bytesToMb(),
                    isLowMemory = isLowMemory
                )
            )
        }
    }
}
