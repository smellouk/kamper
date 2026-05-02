package com.smellouk.kamper.gpu.repository.source

import com.smellouk.kamper.gpu.GpuInfo

/**
 * Internal platform-agnostic interface returning a single GpuInfo sample.
 * Unlike CpuInfoSource, no DTO/delta layer is needed — GPU utilization is
 * a point-in-time reading not a rate calculated from cumulative counters.
 */
internal interface GpuInfoSource {
    fun getInfo(): GpuInfo
}
