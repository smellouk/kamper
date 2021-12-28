package com.smellouk.kamper.memory

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

class MemoryConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = MemoryConfig(true, 1000, Logger.EMPTY)
    }

    class Builder {
        companion object {
            val DEFAULT_ACTION: Builder.() -> Unit = {}
            val DEFAULT: Builder = Builder()
        }

        var intervalInMs: Long = MemoryConfig.DEFAULT.intervalInMs
        var isEnabled: Boolean = MemoryConfig.DEFAULT.isEnabled
        var logger: Logger = MemoryConfig.DEFAULT.logger

        fun build(): MemoryConfig = MemoryConfig(isEnabled, intervalInMs, logger)
    }
}
