package com.smellouk.konitor.gpu.repository.source

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

/**
 * JVM bridge to IOKit IOAccelerator — mirrors the cinterop konitor_gpu_stats()
 * used by macosMain. Loaded only on macOS at runtime; silently returns -1.0 elsewhere
 * or if the framework fails to load.
 *
 * CF memory rules: only release objects we Create/Copy. CFDictionaryGetValue returns
 * a non-retained reference — do NOT CFRelease those values.
 */
internal object MacOsIoKitGpuSource {

    data class GpuStats(
        val utilization: Double,
        val rendererUtilization: Double,
        val tilerUtilization: Double
    )

    @Suppress("FunctionNaming")
    private interface IOKit : Library {
        fun IOServiceMatching(name: String): Pointer?
        fun IOServiceGetMatchingServices(
            masterPort: Int,
            matching: Pointer?,
            existing: IntByReference?
        ): Int
        fun IOIteratorNext(iterator: Int): Int
        fun IORegistryEntryCreateCFProperties(
            entry: Int,
            properties: PointerByReference?,
            allocator: Pointer?,
            options: Int
        ): Int
        fun IOObjectRelease(obj: Int): Int
    }

    @Suppress("FunctionNaming")
    private interface CoreFoundation : Library {
        fun CFStringCreateWithCString(alloc: Pointer?, cStr: String, encoding: Int): Pointer?
        fun CFDictionaryGetValue(theDict: Pointer?, key: Pointer?): Pointer?
        fun CFNumberGetValue(number: Pointer?, theType: Int, valuePtr: Pointer?): Boolean
        fun CFRelease(cf: Pointer?)
    }

    private val iokit: IOKit? =
        runCatching { Native.load("IOKit", IOKit::class.java) }.getOrNull()
    private val cf: CoreFoundation? =
        runCatching { Native.load("CoreFoundation", CoreFoundation::class.java) }.getOrNull()

    fun getStats(): GpuStats {
        val iokit = iokit ?: return UNKNOWN_STATS
        val cf = cf ?: return UNKNOWN_STATS
        return try {
            readStats(iokit, cf)
        } catch (_: Exception) {
            UNKNOWN_STATS
        }
    }

    private fun readStats(iokit: IOKit, cf: CoreFoundation): GpuStats {
        val matching = iokit.IOServiceMatching("IOAccelerator") ?: return UNSUPPORTED_STATS
        val iterRef = IntByReference()
        if (iokit.IOServiceGetMatchingServices(0, matching, iterRef) != KERN_SUCCESS) {
            return UNSUPPORTED_STATS
        }
        return drainIter(iokit, cf, iterRef.value)
    }

    private fun drainIter(iokit: IOKit, cf: CoreFoundation, iter: Int): GpuStats {
        var bestUtil = UNSUPPORTED_SENTINEL
        var bestRenderer = -1.0
        var bestTiler = -1.0
        try {
            var svc: Int
            while (iokit.IOIteratorNext(iter).also { svc = it } != 0) {
                val triple = readServiceStats(iokit, cf, svc)
                if (triple.first >= 0.0 && triple.first > bestUtil) {
                    bestUtil = triple.first.coerceAtMost(MAX_PCT)
                    bestRenderer = bestOf(bestRenderer, triple.second)
                    bestTiler = bestOf(bestTiler, triple.third)
                }
            }
        } finally {
            iokit.IOObjectRelease(iter)
        }
        return GpuStats(bestUtil, bestRenderer, bestTiler)
    }

    private fun bestOf(current: Double, candidate: Double): Double =
        if (candidate >= 0.0) candidate.coerceAtMost(MAX_PCT) else current

    private fun readServiceStats(
        iokit: IOKit,
        cf: CoreFoundation,
        svc: Int
    ): Triple<Double, Double, Double> {
        try {
            val propsRef = PointerByReference()
            if (iokit.IORegistryEntryCreateCFProperties(svc, propsRef, null, 0) != KERN_SUCCESS) {
                return UNKNOWN_TRIPLE
            }
            val props = propsRef.value
            try {
                return readPerfStats(cf, props)
            } finally {
                cf.CFRelease(props)
            }
        } finally {
            iokit.IOObjectRelease(svc)
        }
    }

    private fun readPerfStats(cf: CoreFoundation, props: Pointer): Triple<Double, Double, Double> {
        val perfKey = cf.CFStringCreateWithCString(null, "PerformanceStatistics", CF_STRING_UTF8)
            ?: return UNKNOWN_TRIPLE
        return try {
            val perf = cf.CFDictionaryGetValue(props, perfKey) ?: return UNKNOWN_TRIPLE
            val util = readCfDouble(cf, perf, "Device Utilization %")
                .takeIf { it >= 0.0 } ?: readCfDouble(cf, perf, "GPU Activity(%)")
            val renderer = readCfDouble(cf, perf, "Renderer Utilization %")
            val tiler = readCfDouble(cf, perf, "Tiler Utilization %")
            Triple(util, renderer, tiler)
        } finally {
            cf.CFRelease(perfKey)
        }
    }

    private fun readCfDouble(cf: CoreFoundation, dict: Pointer, key: String): Double {
        val cfKey = cf.CFStringCreateWithCString(null, key, CF_STRING_UTF8) ?: return -1.0
        return try {
            val numPtr = cf.CFDictionaryGetValue(dict, cfKey) ?: return -1.0
            val mem = Memory(DOUBLE_SIZE_BYTES)
            if (cf.CFNumberGetValue(numPtr, CF_NUMBER_DOUBLE_TYPE, mem)) mem.getDouble(0) else -1.0
        } finally {
            cf.CFRelease(cfKey)
        }
    }

    private val UNKNOWN_TRIPLE = Triple(-1.0, -1.0, -1.0)
    private val UNKNOWN_STATS = GpuStats(-1.0, -1.0, -1.0)
    private val UNSUPPORTED_STATS = GpuStats(UNSUPPORTED_SENTINEL, -1.0, -1.0)

    private const val KERN_SUCCESS = 0
    private const val CF_STRING_UTF8 = 0x08000100
    private const val CF_NUMBER_DOUBLE_TYPE = 13
    private const val DOUBLE_SIZE_BYTES = 8L
    private const val MAX_PCT = 100.0
    private const val UNSUPPORTED_SENTINEL = -2.0
}
