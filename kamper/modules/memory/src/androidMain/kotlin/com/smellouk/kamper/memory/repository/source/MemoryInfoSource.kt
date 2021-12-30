package com.smellouk.kamper.memory.repository.source

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Process
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.memory.repository.MemoryInfoDto

internal class MemoryInfoSource(
    context: Context?,
    private val logger: Logger
) {
    private val activityManager: ActivityManager? by lazy {
        context?.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager?
    }

    fun getMemoryInfoDto(): MemoryInfoDto {
        if (activityManager == null) {
            logger.log("ActivityManager is null!")
            return MemoryInfoDto.INVALID
        }

        return try {
            val runtime = RuntimeWrapper.getRuntimeInfo()
            val pssInfo = PssInfoWrapper.getPssInfo(activityManager)
            val ramInfo = RamInfoWrapper.getRamInfo(activityManager)

            MemoryInfoDto(
                // App
                freeMemoryInBytes = runtime.freeMemory,
                maxMemoryInBytes = runtime.maxMemory,
                allocatedInBytes = runtime.allocatedInBytes,
                // PSS
                totalPssInBytes = pssInfo?.totalPss,
                dalvikPssInBytes = pssInfo?.dalvikPss,
                nativePssInBytes = pssInfo?.nativePss,
                otherPssInBytes = pssInfo?.otherPss,
                // Ram
                availableRamInBytes = ramInfo.availMem,
                totalRamInBytes = ramInfo.totalMem,
                lowRamThresholdInBytes = ramInfo.threshold,
                isLowMemory = ramInfo.lowMemory
            )
        } catch (throwable: Throwable) {
            logger.log(throwable.stackTraceToString())
            MemoryInfoDto.INVALID
        }
    }
}

internal class RuntimeWrapper(runtime: Runtime) {
    val freeMemory: Long = runtime.freeMemory()
    val maxMemory: Long = runtime.maxMemory()
    val allocatedInBytes: Long = runtime.totalMemory() - runtime.freeMemory()

    companion object {
        fun getRuntimeInfo(): RuntimeWrapper = RuntimeWrapper(Runtime.getRuntime())
    }
}

internal class PssInfoWrapper(debugMemoryInfo: Debug.MemoryInfo) {
    val totalPss: Long = debugMemoryInfo.totalPss.toLong()
    val dalvikPss: Long = debugMemoryInfo.dalvikPss.toLong()
    val nativePss: Long = debugMemoryInfo.nativePss.toLong()
    val otherPss: Long = debugMemoryInfo.otherPss.toLong()

    companion object {
        fun getPssInfo(activityManager: ActivityManager?): PssInfoWrapper? =
            activityManager?.getProcessMemoryInfo(
                intArrayOf(Process.myPid())
            )?.firstOrNull()?.let {
                PssInfoWrapper(it)
            }
    }
}

internal class RamInfoWrapper(activityMemoryInfo: ActivityManager.MemoryInfo) {
    val availMem: Long = activityMemoryInfo.availMem
    val totalMem: Long = activityMemoryInfo.totalMem
    val threshold: Long = activityMemoryInfo.threshold
    val lowMemory: Boolean = activityMemoryInfo.lowMemory

    companion object {
        fun getRamInfo(activityManager: ActivityManager?): RamInfoWrapper =
            ActivityManager.MemoryInfo().apply {
                activityManager?.getMemoryInfo(this)
            }.let {
                RamInfoWrapper(it)
            }
    }
}
