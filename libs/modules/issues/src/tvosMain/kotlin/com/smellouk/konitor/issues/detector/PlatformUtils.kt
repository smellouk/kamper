package com.smellouk.konitor.issues.detector

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_REALTIME
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
internal actual fun currentPlatformTimeMs(): Long = memScoped {
    val ts = alloc<timespec>()
    clock_gettime(CLOCK_REALTIME.toUInt(), ts.ptr)
    ts.tv_sec * 1_000L + ts.tv_nsec / 1_000_000L
}

internal actual fun captureCurrentStackTrace(): String = ""
