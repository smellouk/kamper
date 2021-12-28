package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Watcher
import com.smellouk.kamper.fps.repository.source.FpsChoreographer

internal actual class FpsPerformance actual constructor(
    watcher: FpsWatcher
) : Performance<FpsConfig, Watcher<FpsInfo>, FpsInfo>(watcher) {
    override fun start() {
        super.start()
        FpsChoreographer.start()
    }

    override fun stop() {
        super.stop()
        FpsChoreographer.stop()
    }
}
