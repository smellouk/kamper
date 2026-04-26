package com.smellouk.kamper.api

internal actual fun currentApiTimeMs(): Long = kotlin.js.Date().getTime().toLong()
