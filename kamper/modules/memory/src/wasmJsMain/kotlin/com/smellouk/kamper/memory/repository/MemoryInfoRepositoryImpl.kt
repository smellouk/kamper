package com.smellouk.kamper.memory.repository

import com.smellouk.kamper.memory.MemoryInfo
import com.smellouk.kamper.memory.repository.source.JsMemoryInfoSource

internal class MemoryInfoRepositoryImpl(
    private val memoryInfoSource: JsMemoryInfoSource,
    private val memoryInfoMapper: MemoryInfoMapper
) : MemoryInfoRepository {
    override fun getInfo(): MemoryInfo = memoryInfoMapper.map(memoryInfoSource.getMemoryInfoDto())
}
