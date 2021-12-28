package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher

internal expect class FpsPerformance(
    watcher: FpsWatcher
) : Performance<FpsConfig, Watcher<FpsInfo>, FpsInfo>
