package com.smellouk.kamper.cpu.repository

import android.os.Build
import android.util.Log
import com.smellouk.kamper.cpu.CpuInfo
import com.smellouk.kamper.cpu.repository.source.CpuInfoSource
import java.io.FileInputStream

internal class CpuInfoRepositoryImpl(
    private val procCpuInfoSource: CpuInfoSource,
    private val shellCpuInfoSource: CpuInfoSource,
    private val cpuInfoMapper: CpuInfoMapper
) : CpuInfoRepository {

    override fun getInfo(): CpuInfo {
        val apiLevel = ApiLevelProvider.getApiLevel()
        val procStatOk = if (apiLevel >= Build.VERSION_CODES.O) ProcStatAccessibilityProvider.isAccessible() else true
        val source = when {
            apiLevel < Build.VERSION_CODES.O -> "proc (API $apiLevel < 26)"
            procStatOk -> "proc (/proc/stat accessible)"
            else -> "shell (top)"
        }
        Log.d("Kamper/CPU", "source=$source apiLevel=$apiLevel procStatOk=$procStatOk")

        val dto = when {
            apiLevel < Build.VERSION_CODES.O -> procCpuInfoSource.getCpuInfoDto()
            procStatOk -> procCpuInfoSource.getCpuInfoDto()
            else -> shellCpuInfoSource.getCpuInfoDto()
        }
        Log.d("Kamper/CPU", "dto=$dto")

        val info = cpuInfoMapper.map(dto)
        Log.d("Kamper/CPU", "info=$info")
        return info
    }
}

internal object ApiLevelProvider {
    fun getApiLevel(): Int = Build.VERSION.SDK_INT
}

// /proc/stat is restricted by SELinux on API 26+ on stock Android,
// but many OEM devices still permit access. Validate actual data to avoid
// accepting files that open but return zeros.
internal object ProcStatAccessibilityProvider {
    fun isAccessible(): Boolean = try {
        val line = FileInputStream("/proc/stat").bufferedReader().use { it.readLine() }
            ?: return false
        val parts = line.trim().split("\\s+".toRegex())
        parts.size >= 5 && parts.drop(1).any { it.toLongOrNull() ?: 0L > 0L }
    } catch (e: Exception) {
        false
    }
}
