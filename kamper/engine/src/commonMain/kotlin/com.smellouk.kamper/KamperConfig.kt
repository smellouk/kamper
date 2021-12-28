package com.smellouk.kamper

import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

class KamperConfig internal constructor(
    val logger: Logger
) {
    companion object {
        val DEFAULT = KamperConfig(Logger.EMPTY)
    }

    object Builder {
        var logger: Logger = DEFAULT.logger
        fun build(): KamperConfig = KamperConfig(logger)
    }
}
