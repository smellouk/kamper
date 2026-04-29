package com.smellouk.kamper.network.repository

import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.repository.source.NetworkInfoSource

internal class NetworkInfoRepositoryImpl(
    private val networkInfoSource: NetworkInfoSource,
    private val networkInfoMapper: NetworkInfoMapper
) : NetworkInfoRepository {
    override fun getInfo(): NetworkInfo =
        networkInfoMapper.map(networkInfoSource.getNetworkInfoDto())
}
