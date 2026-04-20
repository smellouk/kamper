package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Cleanable
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.fps.repository.source.FpsChoreographer

internal class FpsPerformance(
    watcher: IWatcher<FpsInfo>,
    logger: Logger
) : Performance<FpsConfig, IWatcher<FpsInfo>, FpsInfo>(watcher, logger), Cleanable {
    override fun start() {
        super.start()
        FpsChoreographer.start()
    }

    override fun stop() {
        super.stop()
        FpsChoreographer.stop()
    }

    override fun clean() {
        FpsChoreographer.clean()
    }
}
