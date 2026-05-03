package com.smellouk.konitor.api

actual val Logger.Companion.DEFAULT: Logger
    get() = SIMPLE
