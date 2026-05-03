package com.smellouk.konitor.jank

import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.jank.repository.source.JvmJankInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val JankModule: PerformanceModule<JankConfig, JankInfo>
    get() = PerformanceModule(
        config = JankConfig.DEFAULT,
        performance = createPerformance(JankConfig.DEFAULT.logger)
    )

@Suppress("FunctionNaming")
fun JankModule(
    builder: JankConfig.Builder.() -> Unit
): PerformanceModule<JankConfig, JankInfo> = with(JankConfig.Builder().apply(builder).build()) {
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
