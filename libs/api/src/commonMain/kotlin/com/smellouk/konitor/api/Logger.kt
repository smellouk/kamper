package com.smellouk.konitor.api

interface Logger {
    fun log(message: String)

    companion object
}

/**
 * Will use console to show logs
 */
val Logger.Companion.SIMPLE: Logger
    get() = object : Logger {
        override fun log(message: String) {
            println("Konitor: $message")
        }
    }

/**
 * Will not print anything
 */
val Logger.Companion.EMPTY: Logger
    get() = object : Logger {
        override fun log(message: String) {
            /* NO-OP */
        }
    }

/**
 * Will print logs based on platform
 */
expect val Logger.Companion.DEFAULT: Logger
