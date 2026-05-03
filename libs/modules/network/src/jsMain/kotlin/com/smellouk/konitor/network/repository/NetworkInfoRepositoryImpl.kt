package com.smellouk.konitor.network.repository

import com.smellouk.konitor.network.NetworkInfo
import com.smellouk.konitor.network.repository.source.JsNetworkInfoSource

internal class NetworkInfoRepositoryImpl(
    private val networkInfoSource: JsNetworkInfoSource,
    private val networkInfoMapper: NetworkInfoMapper
) : NetworkInfoRepository {
    override fun getInfo(): NetworkInfo = networkInfoMapper.map(networkInfoSource.getNetworkInfoDto())
}
