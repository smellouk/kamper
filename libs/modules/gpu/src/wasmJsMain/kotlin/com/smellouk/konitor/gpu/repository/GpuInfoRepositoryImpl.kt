package com.smellouk.konitor.gpu.repository

import com.smellouk.konitor.gpu.GpuInfo

// No public browser API exposes GPU utilization. WebGL EXT_disjoint_timer_query is
// disabled by default in modern browsers (Spectre mitigation); WebGPU adapter info
// exposes only the adapter name. Returns UNSUPPORTED unconditionally per D-08.
internal class GpuInfoRepositoryImpl : GpuInfoRepository {
    override fun getInfo(): GpuInfo = GpuInfo.UNSUPPORTED
}
