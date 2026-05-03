package com.smellouk.konitor.api

internal actual fun currentApiTimeMs(): Long = kotlin.js.Date().getTime().toLong()
