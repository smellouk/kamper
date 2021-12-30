package com.smellouk.kamper.network.repository

import com.smellouk.kamper.network.repository.source.NetworkInfoSource
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class NetworkInfoRepositoryImplTest {
    private val networkInfoSource = mockk<NetworkInfoSource>(relaxed = true)
    private val networkInfoMapper = mockk<NetworkInfoMapper>(relaxed = true)

    private val classToTest: NetworkInfoRepositoryImpl by lazy {
        NetworkInfoRepositoryImpl(
            networkInfoSource,
            networkInfoMapper
        )
    }

    @Test
    fun `getInfo should call MemoryInfoSource and MemoryInfoMapper`() {
        classToTest.getInfo()

        verify { networkInfoSource.getNetworkInfoDto() }
        verify { networkInfoMapper.map(any()) }
        confirmVerified(networkInfoSource, networkInfoMapper)
    }
}
