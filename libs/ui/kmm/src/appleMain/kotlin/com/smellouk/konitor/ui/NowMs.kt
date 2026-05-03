package com.smellouk.konitor.ui

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
internal actual fun nowMs(): Long = time(null) * 1000L
