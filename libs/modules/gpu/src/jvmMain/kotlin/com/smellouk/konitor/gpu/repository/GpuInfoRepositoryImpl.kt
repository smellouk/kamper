package com.smellouk.konitor.gpu.repository

import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.repository.source.GpuInfoSource

internal class GpuInfoRepositoryImpl(
    private val source: GpuInfoSource
) : GpuInfoRepository {
    override fun getInfo(): GpuInfo = source.getInfo()
}
