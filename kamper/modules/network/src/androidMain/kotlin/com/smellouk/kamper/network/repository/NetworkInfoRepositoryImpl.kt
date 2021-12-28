package com.smellouk.kamper.network.repository

import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.repository.source.NetworkInfoSource

internal class NetworkInfoRepositoryImpl(
    private val networkSource: NetworkInfoSource,
    private val networkInfoMapper: NetworkInfoMapper
) : NetworkInfoRepository {
    override fun getInfo(): NetworkInfo =
        networkInfoMapper.map(networkSource.getNetworkInfoDto())
}
