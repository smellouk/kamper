package com.smellouk.konitor.memory.repository

import com.smellouk.konitor.memory.repository.source.MemoryInfoSource
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
    fun getInfo_should_call_MemoryInfoSource_and_MemoryInfoMapper() {
        classToTest.getInfo()

        verify { memoryInfoSource.getMemoryInfoDto() }
        verify { memoryInfoMapper.map(any()) }
        verifyNoMoreCalls(memoryInfoSource, memoryInfoMapper)
    }
}
