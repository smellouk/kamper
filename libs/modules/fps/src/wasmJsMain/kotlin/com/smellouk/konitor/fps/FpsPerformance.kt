package com.smellouk.konitor.fps

import com.smellouk.konitor.api.Cleanable
import com.smellouk.konitor.api.IWatcher
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.Performance
import com.smellouk.konitor.fps.repository.source.JsFpsTimer

internal class FpsPerformance(
    watcher: IWatcher<FpsInfo>,
    logger: Logger
) : Performance<FpsConfig, IWatcher<FpsInfo>, FpsInfo>(watcher, logger), Cleanable {

    override fun start() {
        super.start()
        JsFpsTimer.start()
    }

    override fun stop() {
        super.stop()
        JsFpsTimer.stop()
    }

    override fun clean() {
        JsFpsTimer.clean()
    }
}
