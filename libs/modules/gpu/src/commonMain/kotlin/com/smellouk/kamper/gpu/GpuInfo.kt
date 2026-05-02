package com.smellouk.kamper.gpu

import com.smellouk.kamper.api.Info

/**
 * GPU performance sample per D-01:
 * @property utilization System-wide GPU %, 0..100, or -1.0 (unknown) / -2.0 (UNSUPPORTED).
 * @property usedMemoryMb GPU memory used in MB, or -1.0 if unknown / -2.0 if unsupported.
 * @property totalMemoryMb Total GPU memory in MB, or -1.0 if unknown / -2.0 if unsupported.
 * @property curFreqKhz Current GPU clock in KHz, or -1L if unknown / -2L if unsupported.
 * @property maxFreqKhz Max GPU clock in KHz, or -1L if unknown / -2L if unsupported.
 * @property appUtilization This process's GPU busy-time %, or -1.0 if unknown.
 * @property rendererUtilization Renderer/fragment engine %, or -1.0 if unknown.
 * @property tilerUtilization Vertex-tiler engine %, or -1.0 if unknown.
 * @property computeUtilization Compute engine %, or -1.0 if unknown.
 *
 * Partial-data semantics (D-02): any field may independently be -1.0/-1L (unknown)
 * while other fields hold real values.
 */
data class GpuInfo(
    val utilization: Double,
    val usedMemoryMb: Double,
    val totalMemoryMb: Double,
    val curFreqKhz: Long = -1L,
    val maxFreqKhz: Long = -1L,
    val appUtilization: Double = -1.0,
    val rendererUtilization: Double = -1.0,
    val tilerUtilization: Double = -1.0,
    val computeUtilization: Double = -1.0
) : Info {
    companion object {
        /** Transient runtime read failure on a supported platform (D-03 / D-13). */
        val INVALID = GpuInfo(
            utilization = -1.0, usedMemoryMb = -1.0, totalMemoryMb = -1.0,
            curFreqKhz = -1L, maxFreqKhz = -1L,
            appUtilization = -1.0, rendererUtilization = -1.0,
            tilerUtilization = -1.0, computeUtilization = -1.0
        )
        /** Permanent platform capability gap — distinct from INVALID (D-04 / D-09 / D-13). */
        val UNSUPPORTED = GpuInfo(
            utilization = -2.0, usedMemoryMb = -2.0, totalMemoryMb = -2.0,
            curFreqKhz = -2L, maxFreqKhz = -2L,
            appUtilization = -2.0, rendererUtilization = -2.0,
            tilerUtilization = -2.0, computeUtilization = -2.0
        )
    }
}
