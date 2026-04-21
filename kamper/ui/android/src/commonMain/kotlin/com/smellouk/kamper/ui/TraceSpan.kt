package com.smellouk.kamper.ui

data class TraceSpan(
    val name: String,
    val startMs: Long,
    val durationMs: Long,
    val depth: Int
)
