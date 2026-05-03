package com.smellouk.konitor.fps

import com.smellouk.konitor.api.Cleanable
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.fps.repository.source.MacosFpsTimer

internal class FpsPerformance(
    watcher: IWatcher<FpsInfo>,
    logger: Logger
) : Performance<FpsConfig, IWatcher<FpsInfo>, FpsInfo>(watcher, logger), Cleanable {

    override fun start() {
        super.start()
        MacosFpsTimer.start()
    }

    override fun stop() {
        super.stop()
        MacosFpsTimer.stop()
    }

    override fun clean() {
        MacosFpsTimer.clean()
    }
}
