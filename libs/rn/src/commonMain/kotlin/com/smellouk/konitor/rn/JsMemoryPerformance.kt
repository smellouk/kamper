package com.smellouk.konitor.rn

import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance

internal class JsMemoryPerformance(
    watcher: IWatcher<JsMemoryInfo>,
    logger: Logger
) : Performance<JsMemoryConfig, IWatcher<JsMemoryInfo>, JsMemoryInfo>(watcher, logger)
