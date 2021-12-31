package com.smellouk.kamper.network

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.network.repository.NetworkInfoMapper
import com.smellouk.kamper.network.repository.NetworkInfoRepositoryImpl
import com.smellouk.kamper.network.repository.source.NetworkInfoSource
import kotlinx.coroutines.Dispatchers

actual val NetworkModule: PerformanceModule<NetworkConfig, NetworkInfo> = PerformanceModule(
    config = NetworkConfig.DEFAULT,
    performance = createPerformance(NetworkConfig.DEFAULT.logger)
)

@KamperDslMarker
@Suppress("FunctionNaming")
fun NetworkModule(
    builder: NetworkConfig.Builder.() -> Unit
): PerformanceModule<NetworkConfig, NetworkInfo> =
    with(NetworkConfig.Builder.apply(builder).build()) {
        PerformanceModule(
            config = this,
            performance = createPerformance(logger)
        )
    }

private fun createPerformance(
    logger: Logger
): Performance<NetworkConfig, Watcher<NetworkInfo>, NetworkInfo> = NetworkPerformance(
    watcher = NetworkWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        repository = NetworkInfoRepositoryImpl(
            networkInfoSource = NetworkInfoSource(logger),
            networkInfoMapper = NetworkInfoMapper()
        ),
        logger = logger
    ),
    logger = logger
)
