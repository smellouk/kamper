package com.smellouk.kamper.gpu.repository

import com.smellouk.kamper.gpu.GpuInfo
import com.smellouk.kamper.gpu.repository.source.GpuInfoSource

internal class GpuInfoRepositoryImpl(
    private val source: GpuInfoSource
) : GpuInfoRepository {
    override fun getInfo(): GpuInfo = source.getInfo()
}
