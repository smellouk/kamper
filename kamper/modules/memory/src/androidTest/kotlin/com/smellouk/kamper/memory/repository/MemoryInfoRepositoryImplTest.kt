package com.smellouk.kamper.memory.repository

import com.smellouk.kamper.memory.repository.source.MemoryInfoSource
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class MemoryInfoRepositoryImplTest {
    private val memoryInfoSource = mockk<MemoryInfoSource>(relaxed = true)
    private val memoryInfoMapper = mockk<MemoryInfoMapper>(relaxed = true)

    private val classToTest: MemoryInfoRepositoryImpl by lazy {
        MemoryInfoRepositoryImpl(
            memoryInfoSource,
            memoryInfoMapper
        )
    }

    @Test
    fun `getInfo should call MemoryInfoSource and MemoryInfoMapper`() {
        classToTest.getInfo()

        verify { memoryInfoSource.getMemoryInfoDto() }
        verify { memoryInfoMapper.map(any()) }
        confirmVerified(memoryInfoSource, memoryInfoMapper)
    }
}
