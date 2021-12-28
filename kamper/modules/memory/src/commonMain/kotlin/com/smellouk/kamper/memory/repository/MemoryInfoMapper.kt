package com.smellouk.kamper.memory.repository

import com.smellouk.kamper.api.toMb
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
                    freeMemoryInMb = freeMemoryInBytes.toMb(),
                    maxMemoryInMb = maxMemoryInBytes.toMb(),
                    allocatedInMb = allocatedInBytes.toMb(),
                ),
                pssInfo = PssInfo(
                    totalPssInMb = totalPssInBytes?.toMb() ?: -1F,
                    dalvikPssInMb = dalvikPssInBytes?.toMb() ?: -1F,
                    nativePssInMb = nativePssInBytes?.toMb() ?: -1F,
                    otherPssInMb = otherPssInBytes?.toMb() ?: -1F
                ),
                ramInfo = RamInfo(
                    availableRamInMb = availableRamInBytes.toMb(),
                    totalRamInMb = totalRamInBytes.toMb(),
                    lowRamThresholdInMb = lowRamThresholdInBytes.toMb(),
                    isLowMemory = isLowMemory
                )
            )
        }
    }
}
