package com.smellouk.konitor.cpu.repository

import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.repository.source.CpuInfoSource

internal class CpuInfoRepositoryImpl(
    private val cpuInfoSource: CpuInfoSource,
    private val cpuInfoMapper: CpuInfoMapper
) : CpuInfoRepository {
    override fun getInfo(): CpuInfo = cpuInfoMapper.map(cpuInfoSource.getCpuInfoDto())
}
