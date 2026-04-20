package com.smellouk.kamper.network.repository

import com.smellouk.kamper.network.repository.source.NetworkInfoSource
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
    fun `getInfo should call MemoryInfoSource and MemoryInfoMapper`() {
        classToTest.getInfo()

        verify { networkInfoSource.getNetworkInfoDto() }
        verify { networkInfoMapper.map(any()) }
        verifyNoMoreCalls(networkInfoSource, networkInfoMapper)
    }
}
