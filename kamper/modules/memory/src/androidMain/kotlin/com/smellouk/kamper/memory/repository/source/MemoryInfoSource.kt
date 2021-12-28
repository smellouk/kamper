package com.smellouk.kamper.memory.repository.source

import android.app.ActivityManager
import android.content.Context
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.memory.repository.MemoryInfoDto

internal class MemoryInfoSource(
    context: Context?,
    private val logger: Logger
) {
    private val activityManager: ActivityManager? =
        context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager?

    fun getMemoryInfoDto(): MemoryInfoDto {
        if (activityManager == null) {
            return MemoryInfoDto.INVALID
        }

        return try {
            val runtime = Runtime.getRuntime()
            val pssInfo = activityManager.getProcessMemoryInfo(
                intArrayOf(android.os.Process.myPid())
            ).firstOrNull()

            val ramInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(ramInfo)

            MemoryInfoDto(
                // App
                freeMemoryInBytes = runtime.freeMemory(),
                maxMemoryInBytes = runtime.maxMemory(),
                allocatedInBytes = runtime.totalMemory() - runtime.freeMemory(),
                // PSS
                totalPssInBytes = pssInfo?.totalPss?.toLong(),
                dalvikPssInBytes = pssInfo?.dalvikPss?.toLong(),
                nativePssInBytes = pssInfo?.nativePss?.toLong(),
                otherPssInBytes = pssInfo?.otherPss?.toLong(),
                // Ram
                availableRamInBytes = ramInfo.availMem,
                totalRamInBytes = ramInfo.totalMem,
                lowRamThresholdInBytes = ramInfo.threshold,
                isLowMemory = ramInfo.lowMemory
            )
        } catch (e: Exception) {
            logger.log(e.stackTraceToString())
            MemoryInfoDto.INVALID
        }
    }
}
