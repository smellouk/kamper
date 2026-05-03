package com.smellouk.konitor.rn

import com.smellouk.konitor.api.Info

data class JsIssueInfo(
    val id: String,
    val message: String,
    val stack: String,
    val isFatal: Boolean,
    val timestampMs: Long
) : Info {
    companion object {
        val INVALID = JsIssueInfo("", "", "", false, -1L)
    }
}
