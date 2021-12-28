package com.smellouk.kamper.cpu.repository

import com.smellouk.kamper.cpu.CpuInfo

class CpuInfoMapper {
    fun map(dto: CpuInfoDto): CpuInfo = if (dto == CpuInfoDto.INVALID) {
        CpuInfo.INVALID
    } else {
        with(dto) {
            val totalTime = total * 1F
            if (totalTime <= 0) {
                return@with CpuInfo.INVALID
            }

            CpuInfo(
                totalUseRatio = (totalTime - idle) / totalTime,
                appRatio = app / totalTime,
                userRatio = user / totalTime,
                systemRatio = system / totalTime,
                ioWaitRatio = ioWait / totalTime
            )
        }
    }
}
