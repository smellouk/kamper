package com.smellouk.kamper.ui

import com.smellouk.kamper.issues.Issue

internal data class IssueRecord(
    val issue: Issue,
    val timestampNs: Long
)
