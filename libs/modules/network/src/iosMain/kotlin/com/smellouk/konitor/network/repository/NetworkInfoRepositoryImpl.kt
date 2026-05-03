package com.smellouk.konitor.network.repository

import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.repository.source.IosNetworkInfoSource

internal class NetworkInfoRepositoryImpl(
    private val networkInfoSource: IosNetworkInfoSource,
    private val networkInfoMapper: NetworkInfoMapper
) : NetworkInfoRepository {
    override fun getInfo(): NetworkInfo = networkInfoMapper.map(networkInfoSource.getNetworkInfoDto())
}
