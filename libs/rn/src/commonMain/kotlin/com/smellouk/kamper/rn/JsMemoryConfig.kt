package com.smellouk.kamper.rn

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger

data class JsMemoryConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = JsMemoryConfig(isEnabled = true, intervalInMs = 1_000L, logger = Logger.EMPTY)
    }

    @KamperDslMarker
    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger
        fun build() = JsMemoryConfig(isEnabled, intervalInMs, logger)
    }
}
