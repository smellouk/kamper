package com.smellouk.kamper

internal actual fun engineCurrentTimeMs(): Long = kotlin.js.Date().getTime().toLong()
