package com.smellouk.konitor.network

import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.network.repository.NetworkInfoMapper
import com.smellouk.konitor.network.repository.NetworkInfoRepositoryImpl
import com.smellouk.konitor.network.repository.source.JsNetworkInfoSource
import kotlinx.coroutines.Dispatchers

actual val NetworkModule: PerformanceModule<NetworkConfig, NetworkInfo>
    get() = PerformanceModule(
        config = NetworkConfig.DEFAULT,
        performance = createPerformance(NetworkConfig.DEFAULT.logger)
    )

@Suppress("FunctionNaming")
fun NetworkModule(
    builder: NetworkConfig.Builder.() -> Unit
): PerformanceModule<NetworkConfig, NetworkInfo> =
    with(NetworkConfig.Builder().apply(builder).build()) {
        PerformanceModule(
            config = this,
            performance = createPerformance(logger)
        )
    }

private fun createPerformance(
    logger: Logger
): Performance<NetworkConfig, IWatcher<NetworkInfo>, NetworkInfo> = NetworkPerformance(
    watcher = NetworkWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = NetworkInfoRepositoryImpl(
            networkInfoSource = JsNetworkInfoSource(),
            networkInfoMapper = NetworkInfoMapper()
        ),
        logger = logger
    ),
    logger = logger
)
