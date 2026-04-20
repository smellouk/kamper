package com.smellouk.kamper.memory.repository

import com.smellouk.kamper.memory.repository.source.MemoryInfoSource
import dev.mokkery.MockMode
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifyNoMoreCalls
import org.junit.Test

class MemoryInfoRepositoryImplTest {
    private val memoryInfoSource = mock<MemoryInfoSource>(MockMode.autofill)
    private val memoryInfoMapper = mock<MemoryInfoMapper>(MockMode.autofill)

    private val classToTest: MemoryInfoRepositoryImpl by lazy {
        MemoryInfoRepositoryImpl(memoryInfoSource, memoryInfoMapper)
    }

    @Test
    fun `getInfo should call MemoryInfoSource and MemoryInfoMapper`() {
        classToTest.getInfo()

        verify { memoryInfoSource.getMemoryInfoDto() }
        verify { memoryInfoMapper.map(any()) }
        verifyNoMoreCalls(memoryInfoSource, memoryInfoMapper)
    }
}
