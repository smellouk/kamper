package com.smellouk.konitor.gpu.repository

import com.smellouk.konitor.gpu.GpuInfo

// IOAccelerator is a private framework on iOS — IOServiceGetMatchingServices returns
// no matching services on iOS devices and using IOKit GPU APIs risks App Store rejection.
// TASK_POWER_INFO_V2.gpu_energy.task_gpu_utilisation was probed but always returns 0;
// the kernel does not populate this counter for sandboxed user-space processes on iOS.
// Returns UNSUPPORTED unconditionally per D-07 + 23-RESEARCH Pitfall 3.
internal class GpuInfoRepositoryImpl : GpuInfoRepository {
    override fun getInfo(): GpuInfo = GpuInfo.UNSUPPORTED
}
