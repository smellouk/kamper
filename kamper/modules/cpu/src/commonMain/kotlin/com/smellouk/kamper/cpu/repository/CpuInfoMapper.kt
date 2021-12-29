package com.smellouk.kamper.cpu.repository

import com.smellouk.kamper.cpu.CpuInfo

internal class CpuInfoMapper {
    fun map(dto: CpuInfoDto): CpuInfo = if (dto == CpuInfoDto.INVALID) {
        CpuInfo.INVALID
    } else {
        with(dto) {
            if (totalTime <= 0) {
                return@with CpuInfo.INVALID
            }

            CpuInfo(
                totalUseRatio = (totalTime - idleTime) / totalTime.toDouble(),
                appRatio = appTime / totalTime.toDouble(),
                userRatio = userTime / totalTime.toDouble(),
                systemRatio = systemTime / totalTime.toDouble(),
                ioWaitRatio = ioWaitTime / totalTime.toDouble()
            )
        }
    }
}
