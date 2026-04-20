package com.smellouk.kamper.cpu.repository

import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.repository.source.CpuInfoSource

internal class CpuInfoRepositoryImpl(
    private val cpuInfoSource: CpuInfoSource,
    private val cpuInfoMapper: CpuInfoMapper
) : CpuInfoRepository {
    override fun getInfo(): CpuInfo = cpuInfoMapper.map(cpuInfoSource.getCpuInfoDto())
}
