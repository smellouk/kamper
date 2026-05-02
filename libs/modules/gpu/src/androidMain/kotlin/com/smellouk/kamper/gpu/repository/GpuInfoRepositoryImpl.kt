package com.smellouk.kamper.gpu.repository

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.gpu.GpuInfo
import com.smellouk.kamper.gpu.repository.source.GpuInfoSource

/**
 * Probe-before-read repository (D-05). One-time probe at first getInfo() call;
 * caches result in `platformSupported` (null=unprobed, false=permanently UNSUPPORTED, true=read).
 * Source priority: Pixel (/sys/kernel/gpu/) → Adreno KGSL → Mali devfreq.
 * After a valid sysfs reading, fdinfo breakdown is overlaid if available.
 */
internal class GpuInfoRepositoryImpl(
    private val pixelSource: GpuInfoSource,
    private val kgslSource: GpuInfoSource,
    private val devfreqSource: GpuInfoSource,
    private val fdinfoSource: GpuInfoSource,
    private val logger: Logger
) : GpuInfoRepository {
    private var platformSupported: Boolean? = null

    override fun getInfo(): GpuInfo {
        if (platformSupported == false) return GpuInfo.UNSUPPORTED
        if (platformSupported == null) {
            val pixelOk = PixelAccessibilityProvider.isAccessible()
            val kgslOk = KgslAccessibilityProvider.isAccessible()
            val devfreqDir = DevfreqAccessibilityProvider.findMaliDir()
            logger.log(
                "[GPU] probe: pixel=$pixelOk kgsl=$kgslOk " +
                    "devfreq=${devfreqDir?.absolutePath ?: "none"}"
            )
            platformSupported = pixelOk || kgslOk || (devfreqDir != null)
            if (platformSupported == false) {
                logger.log("[GPU] no readable sysfs path found — UNSUPPORTED on this device")
                return GpuInfo.UNSUPPORTED
            }
        }
        val sysInfo = resolveSysInfo()
        if (sysInfo == GpuInfo.INVALID || sysInfo == GpuInfo.UNSUPPORTED) return sysInfo
        val breakdown = fdinfoSource.getInfo()
        return if (breakdown == GpuInfo.INVALID) {
            sysInfo
        } else {
            sysInfo.copy(
                appUtilization = breakdown.appUtilization,
                rendererUtilization = breakdown.rendererUtilization,
                tilerUtilization = breakdown.tilerUtilization,
                computeUtilization = breakdown.computeUtilization
            )
        }
    }

    private fun resolveSysInfo(): GpuInfo {
        val pixel = pixelSource.getInfo()
        if (pixel != GpuInfo.INVALID) return pixel
        val kgsl = kgslSource.getInfo()
        return if (kgsl != GpuInfo.INVALID) kgsl else devfreqSource.getInfo()
    }
}
