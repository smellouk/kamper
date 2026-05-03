package com.smellouk.konitor.network.repository

import com.smellouk.konitor.network.repository.source.NetworkInfoSource
import dev.mokkery.MockMode
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verifyNoMoreCalls
import org.junit.Test

class NetworkInfoRepositoryImplTest {
    private val networkInfoSource = mock<NetworkInfoSource>(MockMode.autofill)
    private val networkInfoMapper = mock<NetworkInfoMapper>(MockMode.autofill)

    private val classToTest: NetworkInfoRepositoryImpl by lazy {
        NetworkInfoRepositoryImpl(networkInfoSource, networkInfoMapper)
    }

    @Test
    fun getInfo_should_call_MemoryInfoSource_and_MemoryInfoMapper() {
        classToTest.getInfo()

        verify { networkInfoSource.getNetworkInfoDto() }
        verify { networkInfoMapper.map(any()) }
        verifyNoMoreCalls(networkInfoSource, networkInfoMapper)
    }
}
