package com.smellouk.konitor.ui

import com.smellouk.konitor.issues.Issue

internal data class IssueRecord(
    val issue: Issue,
    val timestampNs: Long
)
