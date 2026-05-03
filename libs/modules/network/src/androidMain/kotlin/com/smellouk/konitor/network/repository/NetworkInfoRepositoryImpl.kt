package com.smellouk.konitor.network.repository

import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.repository.source.NetworkInfoSource

internal class NetworkInfoRepositoryImpl(
    private val networkInfoSource: NetworkInfoSource,
    private val networkInfoMapper: NetworkInfoMapper
) : NetworkInfoRepository {
    override fun getInfo(): NetworkInfo =
        networkInfoMapper.map(networkInfoSource.getNetworkInfoDto())
}
