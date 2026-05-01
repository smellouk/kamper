package com.smellouk.kamper.rn

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance

internal class JsMemoryPerformance(
    watcher: IWatcher<JsMemoryInfo>,
    logger: Logger
) : Performance<JsMemoryConfig, IWatcher<JsMemoryInfo>, JsMemoryInfo>(watcher, logger)
