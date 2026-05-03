@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package com.smellouk.konitor.fps.repository.source

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.darwin.NSObject
import platform.Foundation.NSDefaultRunLoopMode
import platform.Foundation.NSRunLoop
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CADisplayLink
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

@OptIn(kotlin.experimental.ExperimentalObjCName::class)
@ObjCName("KonitorFpsDisplayLinkTarget")
internal class DisplayLinkTarget(val callback: () -> Unit) : NSObject() {
    @ObjCAction
    fun tick() { callback() }
}

@OptIn(ExperimentalForeignApi::class)
internal object IosFpsTimer {
    private var displayLink: CADisplayLink? = null
    private var linkTarget: DisplayLinkTarget? = null
    private val frameListeners = mutableListOf<(Long) -> Unit>()

    fun addFrameListener(listener: (Long) -> Unit) {
        if (!frameListeners.contains(listener)) frameListeners.add(listener)
    }

    fun removeFrameListener(listener: (Long) -> Unit) {
        frameListeners.remove(listener)
    }

    fun start() {
        if (displayLink != null) return
        val t = DisplayLinkTarget { val now = currentTimeNanos(); frameListeners.forEach { it(now) } }
        linkTarget = t
        val link = CADisplayLink.displayLinkWithTarget(t, NSSelectorFromString("tick"))
        link.addToRunLoop(NSRunLoop.mainRunLoop(), forMode = NSDefaultRunLoopMode)
        displayLink = link
    }

    fun stop() {
        displayLink?.invalidate()
        displayLink = null
        linkTarget = null
    }

    fun clean() {
        stop()
        frameListeners.clear()
    }

    private fun currentTimeNanos(): Long = memScoped {
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr)
        ts.tv_sec * 1_000_000_000L + ts.tv_nsec
    }
}
