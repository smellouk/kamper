package com.smellouk.konitor.gpu.repository

import java.io.FileInputStream

/**
 * Probes Adreno kgsl sysfs accessibility (D-05).
 * Tries `gpu_busy_percentage` first (pre-computed %), then `gpubusy` (raw ratio format).
 * Both paths expose the same hardware counter — availability varies by kernel/device.
 */
internal object KgslAccessibilityProvider {
    private const val PATH_BUSY_PCT = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
    private const val PATH_GPUBUSY = "/sys/class/kgsl/kgsl-3d0/gpubusy"

    fun isAccessible(): Boolean = isReadable(PATH_BUSY_PCT) || isReadable(PATH_GPUBUSY)

    private fun isReadable(path: String): Boolean = try {
        FileInputStream(path).bufferedReader().use { it.readLine() } != null
    } catch (_: Exception) {
        false
    }
}
