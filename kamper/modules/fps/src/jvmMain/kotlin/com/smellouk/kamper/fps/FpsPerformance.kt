package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Cleanable
import com.smellouk.kamper.api.IWatcher
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.fps.repository.source.JvmFpsTimer

internal class FpsPerformance(
    watcher: IWatcher<FpsInfo>,
    logger: Logger
) : Performance<FpsConfig, IWatcher<FpsInfo>, FpsInfo>(watcher, logger), Cleanable {

    override fun start() {
        super.start()
        JvmFpsTimer.start()
    }

    override fun stop() {
        super.stop()
        JvmFpsTimer.stop()
    }

    override fun clean() {
        JvmFpsTimer.clean()
    }
}
