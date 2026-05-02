package com.smellouk.kamper.gpu.repository

import com.smellouk.kamper.gpu.GpuInfo

// IOAccelerator is a private framework on tvOS — IOServiceGetMatchingServices returns
// no matching services on tvOS devices and using IOKit GPU APIs risks App Store rejection.
// TASK_POWER_INFO_V2.gpu_energy.task_gpu_utilisation was probed but always returns 0;
// the kernel does not populate this counter for sandboxed user-space processes on tvOS.
// Returns UNSUPPORTED unconditionally per D-07 + 23-RESEARCH Pitfall 3.
internal class GpuInfoRepositoryImpl : GpuInfoRepository {
    override fun getInfo(): GpuInfo = GpuInfo.UNSUPPORTED
}
