package com.smellouk.konitor.cpu.repository

import com.smellouk.konitor.cpu.CpuInfo

internal class CpuInfoMapper {
    fun map(dto: CpuInfoDto): CpuInfo = if (dto == CpuInfoDto.INVALID) {
        CpuInfo.INVALID
    } else {
        with(dto) {
            if (totalTime <= 0) {
                return@with CpuInfo.INVALID
            }

            CpuInfo(
                totalUseRatio = (totalTime - idleTime) / totalTime,
                appRatio = appTime / totalTime,
                userRatio = userTime / totalTime,
                systemRatio = systemTime / totalTime,
                ioWaitRatio = ioWaitTime / totalTime
            )
        }
    }
}
