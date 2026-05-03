package com.smellouk.konitor.cpu.repository.source

import com.smellouk.konitor.cpu.repository.CpuInfoDto

internal interface CpuInfoSource {
    fun getCpuInfoDto(): CpuInfoDto
}
