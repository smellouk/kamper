package com.smellouk.konitor.cpu.repository.source

import com.smellouk.konitor.cpu.repository.CpuInfoDto

internal class JsCpuInfoSource : CpuInfoSource {
    init {
        JsCpuSampler.ensureStarted()
    }

    override fun getCpuInfoDto(): CpuInfoDto {
        val load = JsCpuSampler.loadEstimate
        return CpuInfoDto(
            totalTime = 100.0,
            userTime = load * 100.0,
            systemTime = 0.0,
            idleTime = (1.0 - load) * 100.0,
            ioWaitTime = 0.0,
            appTime = load * 100.0
        )
    }
}
