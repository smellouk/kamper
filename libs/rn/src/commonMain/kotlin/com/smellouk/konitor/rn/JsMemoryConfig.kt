package com.smellouk.konitor.rn

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.api.KonitorDslMarker
import com.smellouk.konitor.api.Logger

data class JsMemoryConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = JsMemoryConfig(isEnabled = true, intervalInMs = 1_000L, logger = Logger.EMPTY)
    }

    @KonitorDslMarker
    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger
        fun build() = JsMemoryConfig(isEnabled, intervalInMs, logger)
    }
}
