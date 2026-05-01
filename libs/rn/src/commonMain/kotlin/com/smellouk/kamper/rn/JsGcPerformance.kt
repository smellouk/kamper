package com.smellouk.kamper.rn

import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance

internal class JsGcPerformance(
    watcher: IWatcher<JsGcInfo>,
    logger: Logger
) : Performance<JsGcConfig, IWatcher<JsGcInfo>, JsGcInfo>(watcher, logger)
