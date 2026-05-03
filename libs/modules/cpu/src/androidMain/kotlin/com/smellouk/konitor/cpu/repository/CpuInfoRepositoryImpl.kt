package com.smellouk.konitor.cpu.repository

import android.os.Build
import android.util.Log
import com.smellouk.konitor.cpu.BuildConfig
import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.repository.source.CpuInfoSource
import java.io.FileInputStream

internal class CpuInfoRepositoryImpl(
    private val procCpuInfoSource: CpuInfoSource,
    private val shellCpuInfoSource: CpuInfoSource,
    private val cpuInfoMapper: CpuInfoMapper
) : CpuInfoRepository {

    // Capability cache: null = not yet probed, true = at least one source returned data,
    // false = both proc/stat and shell are unavailable on this device (FEAT-01 D-03).
    private var platformSupported: Boolean? = null

    // Delta sources always return INVALID on the first call (baseline caching). Skip the
    // UNSUPPORTED probe on that warm-up call so we don't permanently cache a false negative.
    private var firstCallComplete = false

    override fun getInfo(): CpuInfo {
        // Early return for cached UNSUPPORTED (D-03 — one-time probe, do not retry OS calls).
        if (platformSupported == false) return CpuInfo.UNSUPPORTED

        val apiLevel = ApiLevelProvider.getApiLevel()
        val procStatOk = if (apiLevel >= Build.VERSION_CODES.O) ProcStatAccessibilityProvider.isAccessible() else true
        if (BuildConfig.DEBUG) {
            val source = when {
                apiLevel < Build.VERSION_CODES.O -> "proc (API $apiLevel < 26)"
                procStatOk -> "proc (/proc/stat accessible)"
                else -> "shell (top)"
            }
            Log.d("Konitor/CPU", "source=$source apiLevel=$apiLevel procStatOk=$procStatOk")
        }

        val dto = when {
            apiLevel < Build.VERSION_CODES.O -> procCpuInfoSource.getCpuInfoDto()
            procStatOk -> procCpuInfoSource.getCpuInfoDto()
            else -> shellCpuInfoSource.getCpuInfoDto()
        }
        if (BuildConfig.DEBUG) Log.d("Konitor/CPU", "dto=$dto")

        // FEAT-01 capability probe: when both proc/stat and the shell fallback are unavailable,
        // the device cannot deliver CPU samples — cache and surface UNSUPPORTED.
        // Skip on the first call: delta sources always return INVALID during baseline caching.
        if (firstCallComplete && dto == CpuInfoDto.INVALID && apiLevel >= Build.VERSION_CODES.O && !procStatOk) {
            platformSupported = false
            if (BuildConfig.DEBUG) {
                Log.d("Konitor/CPU", "platformSupported=false (both sources unavailable) — returning CpuInfo.UNSUPPORTED")
            }
            return CpuInfo.UNSUPPORTED
        }

        firstCallComplete = true

        // Successful sample (or transient INVALID from mapper): mark platform supported (terminal state).
        if (dto != CpuInfoDto.INVALID && platformSupported == null) {
            platformSupported = true
        }

        val info = cpuInfoMapper.map(dto)
        if (BuildConfig.DEBUG) Log.d("Konitor/CPU", "info=$info")
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
            ?: run {
                if (BuildConfig.DEBUG) Log.d("Konitor/CPU", "/proc/stat accessible=false (null line)")
                return false
            }
        val parts = line.trim().split("\\s+".toRegex())
        val result = parts.size >= 5 && parts.drop(1).any { (it.toLongOrNull() ?: 0L) > 0L }
        if (BuildConfig.DEBUG) Log.d("Konitor/CPU", "/proc/stat accessible=$result line='$line'")
        result
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.d("Konitor/CPU", "/proc/stat accessible=false ${e.javaClass.simpleName}: ${e.message}")
        false
    }
}
