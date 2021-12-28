package com.smellouk.kamper.fps

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.fps.repository.FpsInfoMapper
import com.smellouk.kamper.fps.repository.FpsInfoRepositoryImpl
import com.smellouk.kamper.fps.repository.source.FpsChoreographer
import com.smellouk.kamper.fps.repository.source.FpsInfoSource
import kotlinx.coroutines.Dispatchers

actual val FpsModule: PerformanceModule<FpsConfig, FpsInfo> = PerformanceModule(
    config = FpsConfig.DEFAULT,
    performance = createPerformance(FpsConfig.DEFAULT.logger)
)

@KamperDslMarker
@Suppress("FunctionNaming")
fun FpsModule(
    builder: FpsConfig.Builder.() -> Unit
): PerformanceModule<FpsConfig, FpsInfo> = with(FpsConfig.Builder.apply(builder).build()) {
    PerformanceModule(
        config = this,
        performance = createPerformance(logger)
    )
}

private fun createPerformance(
    logger: Logger
): Performance<FpsConfig, Watcher<FpsInfo>, FpsInfo> = FpsPerformance(
    watcher = FpsWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        repository = FpsInfoRepositoryImpl(
            fpsInfoMapper = FpsInfoMapper(),
            fpsSource = FpsInfoSource(FpsChoreographer),
        ),
        logger = logger
    ),
    logger = logger
)
