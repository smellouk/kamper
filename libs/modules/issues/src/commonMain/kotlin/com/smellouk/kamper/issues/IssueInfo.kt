package com.smellouk.kamper.issues

import com.smellouk.kamper.api.Info

data class IssueInfo(val issue: Issue) : Info {
    companion object {
        val INVALID = IssueInfo(Issue.INVALID)
        val UNSUPPORTED = IssueInfo(Issue.UNSUPPORTED)
    }
}
