package com.smellouk.konitor.gpu.repository

import java.io.FileInputStream

/**
 * Probes Google Pixel /sys/kernel/gpu/ GPU stats interface.
 * Google added this on Tensor G1 (Pixel 6) and it persists on later Pixel generations.
 * `gpu_busy` contains the GPU utilization percentage as a plain integer string.
 */
internal object PixelAccessibilityProvider {
    private const val PATH_GPU_BUSY = "/sys/kernel/gpu/gpu_busy"

    fun isAccessible(): Boolean = try {
        FileInputStream(PATH_GPU_BUSY).bufferedReader().use { it.readLine() } != null
    } catch (_: Exception) {
        false
    }
}
