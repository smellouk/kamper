package com.smellouk.konitor.memory.repository

import com.smellouk.konitor.memory.MemoryInfo
import com.smellouk.konitor.memory.repository.source.JvmMemoryInfoSource

internal class MemoryInfoRepositoryImpl(
    private val memoryInfoSource: JvmMemoryInfoSource,
    private val memoryInfoMapper: MemoryInfoMapper
) : MemoryInfoRepository {
    override fun getInfo(): MemoryInfo = memoryInfoMapper.map(memoryInfoSource.getMemoryInfoDto())
}
