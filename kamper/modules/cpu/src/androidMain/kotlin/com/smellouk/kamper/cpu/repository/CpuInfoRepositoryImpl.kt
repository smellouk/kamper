package com.smellouk.kamper.cpu.repository

import android.os.Build
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.repository.source.CpuInfoSource

internal class CpuInfoRepositoryImpl(
    private val procCpuInfoRawSource: CpuInfoSource,
    private val shellCpuInfoRawSource: CpuInfoSource,
    private val cpuInfoMapper: CpuInfoMapper
) : CpuInfoRepository {
    override fun getInfo(): CpuInfo {
        val cpuInfoRaw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            shellCpuInfoRawSource.getCpuInfoDto()
        } else {
            procCpuInfoRawSource.getCpuInfoDto()
        }

        return cpuInfoMapper.map(cpuInfoRaw)
    }
}
