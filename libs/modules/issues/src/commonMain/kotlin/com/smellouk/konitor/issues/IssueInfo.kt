package com.smellouk.konitor.issues

import com.smellouk.konitor.api.Info

data class IssueInfo(val issue: Issue) : Info {
    companion object {
        val INVALID = IssueInfo(Issue.INVALID)
        val UNSUPPORTED = IssueInfo(Issue.UNSUPPORTED)
    }
}
