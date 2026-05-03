package com.smellouk.konitor.rn

import com.smellouk.konitor.api.InfoRepository

internal interface JsMemoryRepository : InfoRepository<JsMemoryInfo>

internal class JsMemoryRepositoryImpl : JsMemoryRepository {
    override fun getInfo(): JsMemoryInfo {
        val (used, total) = JsRuntimeBridge.readMemory()
        return if (used < 0) JsMemoryInfo.INVALID else JsMemoryInfo(used, total)
    }
}
