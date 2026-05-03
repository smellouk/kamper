package com.smellouk.konitor.gpu.repository

import java.io.File

/**
 * Probes Mali GPU sysfs accessibility (D-05 fallback). Checks three path families in order:
 * 1. `/sys/class/devfreq/` — generic devfreq interface (most Mali devices)
 * 2. `/sys/class/misc/mali0/device/` — symlink to platform device (Pixel/Tensor, Mali r32+)
 * 3. `/sys/bus/platform/drivers/mali/` — direct driver binding (alternate Mali layout)
 * Returns the first directory where `cur_freq` is readable.
 */
internal object DevfreqAccessibilityProvider {
    private const val DEVFREQ_ROOT = "/sys/class/devfreq"
    private const val MALI_MISC_DEVICE = "/sys/class/misc/mali0/device"
    private const val MALI_PLATFORM_DRIVERS = "/sys/bus/platform/drivers/mali"

    fun isAccessible(): Boolean = findMaliDir() != null

    fun findMaliDir(): File? =
        findInDevfreq() ?: findInMaliMisc() ?: findInPlatformDrivers()

    private fun findInDevfreq(): File? = try {
        File(DEVFREQ_ROOT).listFiles()?.firstOrNull { entry ->
            val name = entry.name.lowercase()
            (name.contains("mali") || name.contains("gpu")) &&
                File(entry, "cur_freq").canRead()
        }
    } catch (_: Exception) {
        null
    }

    private fun findInMaliMisc(): File? = try {
        val device = File(MALI_MISC_DEVICE)
        if (File(device, "cur_freq").canRead()) device else null
    } catch (_: Exception) {
        null
    }

    private fun findInPlatformDrivers(): File? = try {
        File(MALI_PLATFORM_DRIVERS).listFiles()?.firstOrNull { entry ->
            File(entry, "cur_freq").canRead()
        }
    } catch (_: Exception) {
        null
    }
}
