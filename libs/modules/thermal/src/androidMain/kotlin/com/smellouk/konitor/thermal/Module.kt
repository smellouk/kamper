package com.smellouk.konitor.thermal

import android.content.Context
import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.PerformanceModule
import com.smellouk.konitor.thermal.repository.ThermalInfoRepositoryImpl
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
