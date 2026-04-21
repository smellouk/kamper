package com.smellouk.kamper.jank

import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.jank.repository.source.JvmJankInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val JankModule: PerformanceModule<JankConfig, JankInfo>
    get() = PerformanceModule(
        config = JankConfig.DEFAULT,
        performance = createPerformance(JankConfig.DEFAULT.logger)
    )

@KamperDslMarker
@Suppress("FunctionNaming")
fun JankModule(
    builder: JankConfig.Builder.() -> Unit
): PerformanceModule<JankConfig, JankInfo> = with(JankConfig.Builder.apply(builder).build()) {
    PerformanceModule(config = this, performance = createPerformance(logger))
}

private fun createPerformance(logger: Logger) = JankPerformance(
    watcher = JankWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Default,
        repository = JvmJankInfoRepositoryImpl(),
        logger = logger
    ),
    logger = logger
)
