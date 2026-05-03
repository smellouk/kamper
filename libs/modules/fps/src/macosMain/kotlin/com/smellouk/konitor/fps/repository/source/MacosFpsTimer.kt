package com.smellouk.konitor.fps.repository.source

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(ExperimentalForeignApi::class)
internal object MacosFpsTimer {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private var frameListener: ((Long) -> Unit)? = null

    fun setFrameListener(listener: (Long) -> Unit) {
        frameListener = listener
    }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                frameListener?.invoke(currentTimeNanos())
                delay(FRAME_INTERVAL_MS)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun clean() {
        stop()
        frameListener = null
    }

    private fun currentTimeNanos(): Long = memScoped {
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr)
        ts.tv_sec * 1_000_000_000L + ts.tv_nsec
    }

    private const val FRAME_INTERVAL_MS = 1000L / 60L
}
