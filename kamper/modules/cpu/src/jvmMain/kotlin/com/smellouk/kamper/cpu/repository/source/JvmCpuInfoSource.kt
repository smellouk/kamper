package com.smellouk.kamper.cpu.repository.source

import com.smellouk.kamper.cpu.repository.CpuInfoDto
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

internal class JvmCpuInfoSource : CpuInfoSource {
    private val osBean: OperatingSystemMXBean? = runCatching {
        ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    }.getOrNull()

    override fun getCpuInfoDto(): CpuInfoDto {
        val bean = osBean ?: return CpuInfoDto.INVALID
        val systemCpu = bean.cpuLoad
        val processCpu = bean.processCpuLoad

        if (systemCpu < 0.0 || processCpu < 0.0) return CpuInfoDto.INVALID

        val totalTime = 100.0
        val idleTime = (1.0 - systemCpu) * totalTime
        val appTime = processCpu * totalTime
        val systemTime = maxOf(0.0, (systemCpu - processCpu) * totalTime)

        return CpuInfoDto(
            totalTime = totalTime,
            userTime = appTime,
            systemTime = systemTime,
            idleTime = idleTime,
            ioWaitTime = 0.0,
            appTime = appTime
        )
    }
}
