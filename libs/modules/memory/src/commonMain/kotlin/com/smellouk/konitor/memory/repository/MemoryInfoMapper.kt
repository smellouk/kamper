package com.smellouk.konitor.memory.repository

import com.smellouk.konitor.api.bytesToMb
import com.smellouk.konitor.api.kBytesToMb
import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.MemoryInfo.HeapMemoryInfo
import com.smellouk.konitor.memory.MemoryInfo.PssInfo
import com.smellouk.konitor.memory.MemoryInfo.RamInfo

internal class MemoryInfoMapper {
    fun map(dto: MemoryInfoDto): MemoryInfo = if (dto == MemoryInfoDto.INVALID) {
        MemoryInfo.INVALID
    } else {
        with(dto) {
            MemoryInfo(
                heapMemoryInfo = HeapMemoryInfo(
                    maxMemoryInMb = maxMemoryInBytes.bytesToMb(),
                    allocatedInMb = allocatedInBytes.bytesToMb(),
                ),
                pssInfo = PssInfo(
                    totalPssInMb = totalPssInKiloBytes?.kBytesToMb() ?: -1F,
                    dalvikPssInMb = dalvikPssInKiloBytes?.kBytesToMb() ?: -1F,
                    nativePssInMb = nativePssInKiloBytes?.kBytesToMb() ?: -1F,
                    otherPssInMb = otherPssInKiloBytes?.kBytesToMb() ?: -1F
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
