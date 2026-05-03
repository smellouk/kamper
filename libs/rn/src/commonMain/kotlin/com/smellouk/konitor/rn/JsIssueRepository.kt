package com.smellouk.konitor.rn

import com.smellouk.konitor.api.InfoRepository

internal interface JsIssueRepository : InfoRepository<JsIssueInfo>

internal class JsIssueRepositoryImpl : JsIssueRepository {
    override fun getInfo(): JsIssueInfo {
        val crash = JsRuntimeBridge.drainCrash() ?: return JsIssueInfo.INVALID
        return JsIssueInfo(
            id = generateId(),
            message = crash.first,
            stack = crash.second,
            isFatal = crash.third,
            timestampMs = currentTimeMs()
        )
    }
}

internal expect fun generateId(): String
internal expect fun currentTimeMs(): Long
