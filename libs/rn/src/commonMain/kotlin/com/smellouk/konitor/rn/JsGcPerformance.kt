package com.smellouk.konitor.rn

import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance

internal class JsGcPerformance(
    watcher: IWatcher<JsGcInfo>,
    logger: Logger
) : Performance<JsGcConfig, IWatcher<JsGcInfo>, JsGcInfo>(watcher, logger)
