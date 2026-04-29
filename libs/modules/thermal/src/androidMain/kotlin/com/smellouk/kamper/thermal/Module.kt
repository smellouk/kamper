package com.smellouk.kamper.thermal

import android.content.Context
import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.PerformanceModule
import com.smellouk.kamper.thermal.repository.ThermalInfoRepositoryImpl
import kotlinx.coroutines.Dispatchers

actual val ThermalModule: PerformanceModule<ThermalConfig, ThermalInfo>
    get() = throw IllegalStateException("Use ThermalModule(context) on Android")

@Suppress("FunctionNaming")
fun ThermalModule(
    context: Context,
    builder: ThermalConfig.Builder.() -> Unit = {}
): PerformanceModule<ThermalConfig, ThermalInfo> = with(ThermalConfig.Builder().apply(builder).build()) {
    PerformanceModule(config = this, performance = createPerformance(context, logger))
}

private fun createPerformance(context: Context, logger: Logger) = ThermalPerformance(
    watcher = ThermalWatcher(
        defaultDispatcher = Dispatchers.Default,
        mainDispatcher = Dispatchers.Main,
        repository = ThermalInfoRepositoryImpl(context.applicationContext),
        logger = logger
    ),
    logger = logger
)
