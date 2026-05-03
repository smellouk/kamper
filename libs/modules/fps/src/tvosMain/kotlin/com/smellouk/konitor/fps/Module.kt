package com.smellouk.konitor.fps

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.fps.repository.FpsInfoMapper
import com.smellouk.konitor.fps.repository.FpsInfoRepositoryImpl
import com.smellouk.konitor.fps.repository.source.FpsInfoSource
import kotlinx.coroutines.Dispatchers

actual val FpsModule: PerformanceModule<FpsConfig, FpsInfo>
    get() = PerformanceModule(
        config = FpsConfig.DEFAULT,
        performance = createPerformance(FpsConfig.DEFAULT.logger)
    )

@Suppress("FunctionNaming")
fun FpsModule(
    builder: FpsConfig.Builder.() -> Unit
): PerformanceModule<FpsConfig, FpsInfo> = with(FpsConfig.Builder().apply(builder).build()) {
    PerformanceModule(
        config = this,
        performance = createPerformance(logger)
    )
}

private fun createPerformance(
    logger: Logger
): Performance<FpsConfig, IWatcher<FpsInfo>, FpsInfo> = FpsPerformance(
    watcher = FpsWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = FpsInfoRepositoryImpl(
            fpsInfoMapper = FpsInfoMapper(),
            fpsSource = FpsInfoSource()
        ),
        logger = logger
    ),
    logger = logger
)
