package com.smellouk.kamper.cpu.repository.source

import com.smellouk.kamper.cpu.repository.CpuInfoDto

internal interface CpuInfoSource {
    fun getCpuInfoDto(): CpuInfoDto
}
