package com.smellouk.kamper.samples.web.ui

import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueSpans
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

internal object IssuesSection {
    private lateinit var listEl: HTMLElement
    private lateinit var emptyEl: HTMLElement
    private val issues = mutableListOf<Issue>()

    fun build(parent: HTMLElement) {
        val controls = (document.createElement("div") as HTMLElement).also {
            it.className = "issue-controls"
            parent.appendChild(it)
        }
        controls.button("btn btn-action") {
            textContent = "Slow Span"
            style.color = "#89b4fa"
            style.borderColor = "#89b4fa"
            onclick = {
                IssueSpans.measure("web-demo-op", thresholdMs = 300L) {
                    val end = kotlin.js.Date().getTime() + 800
                    while (kotlin.js.Date().getTime() < end) { /* busy wait */ }
                }
                null
            }
        }
        controls.button("btn btn-action") {
            textContent = "Clear"
            onclick = { clearIssues(); null }
        }


        emptyEl = (document.createElement("p") as HTMLElement).also {
            it.className = "issue-empty"
            it.textContent = "No issues detected"
            parent.appendChild(it)
        }

        listEl = (document.createElement("div") as HTMLElement).also {
            it.className = "issue-list"
            parent.appendChild(it)
        }

        updateVisibility()
    }

    fun addIssue(issue: Issue) {
        issues.add(0, issue)
        if (issues.size > 100) issues.removeAt(issues.size - 1)
        render()
    }

    private fun clearIssues() {
        issues.clear()
        render()
    }

    private fun render() {
        listEl.innerHTML = ""
        issues.forEach { issue ->
            val row = (document.createElement("div") as HTMLElement).also {
                it.className = "issue-row"
                listEl.appendChild(it)
            }
            val bar = (document.createElement("div") as HTMLElement).also {
                it.className = "issue-bar"
                it.style.backgroundColor = severityColor(issue.severity)
                row.appendChild(it)
            }
            bar.className = "issue-bar"

            val content = (document.createElement("div") as HTMLElement).also {
                it.className = "issue-content"
                row.appendChild(it)
            }

            val header = (document.createElement("div") as HTMLElement).also {
                it.className = "issue-header"
                content.appendChild(it)
            }
            (document.createElement("span") as HTMLElement).also {
                it.className = "issue-type-chip"
                it.textContent = typeShortName(issue.type)
                it.style.color = typeColor(issue.type)
                it.style.borderColor = typeColor(issue.type)
                header.appendChild(it)
            }
            (document.createElement("span") as HTMLElement).also {
                it.className = "issue-severity"
                it.textContent = issue.severity.name
                it.style.color = severityColor(issue.severity)
                header.appendChild(it)
            }
            (document.createElement("span") as HTMLElement).also {
                it.className = "issue-time"
                it.textContent = fmtTime(issue.timestampMs)
                header.appendChild(it)
            }
            (document.createElement("div") as HTMLElement).also {
                it.className = "issue-msg"
                it.textContent = issue.message
                content.appendChild(it)
            }

            val details = buildDetails(issue)
            if (details.isNotEmpty()) {
                (document.createElement("div") as HTMLElement).also {
                    it.className = "issue-details"
                    it.textContent = details
                    content.appendChild(it)
                }
            }
        }
        updateVisibility()
    }

    private fun updateVisibility() {
        if (issues.isEmpty()) {
            emptyEl.style.display = "block"
            listEl.style.display = "none"
        } else {
            emptyEl.style.display = "none"
            listEl.style.display = "block"
        }
    }

    private fun severityColor(s: Severity): String = when (s) {
        Severity.CRITICAL -> "#f38ba8"
        Severity.ERROR    -> "#fab387"
        Severity.WARNING  -> "#f9e2af"
        Severity.INFO     -> "#a6e3a1"
    }

    private fun typeColor(t: IssueType): String = when (t) {
        IssueType.ANR, IssueType.CRASH             -> "#f38ba8"
        IssueType.SLOW_COLD_START,
        IssueType.SLOW_WARM_START,
        IssueType.SLOW_HOT_START                   -> "#fab387"
        IssueType.DROPPED_FRAME                    -> "#f9e2af"
        IssueType.SLOW_SPAN                        -> "#89b4fa"
        IssueType.MEMORY_PRESSURE, IssueType.NEAR_OOM -> "#cba6f7"
        IssueType.STRICT_VIOLATION                 -> "#94e2d5"
    }

    private fun typeShortName(t: IssueType): String = when (t) {
        IssueType.ANR              -> "ANR"
        IssueType.SLOW_COLD_START  -> "COLD"
        IssueType.SLOW_WARM_START  -> "WARM"
        IssueType.SLOW_HOT_START   -> "HOT"
        IssueType.DROPPED_FRAME    -> "JANK"
        IssueType.SLOW_SPAN        -> "SPAN"
        IssueType.MEMORY_PRESSURE  -> "MEM"
        IssueType.NEAR_OOM         -> "OOM"
        IssueType.CRASH            -> "CRASH"
        IssueType.STRICT_VIOLATION -> "STRICT"
    }

    private fun buildDetails(issue: Issue): String {
        val parts = mutableListOf<String>()
        issue.durationMs?.let { parts.add("${it}ms") }
        issue.threadName?.let { parts.add("thread=$it") }
        issue.details.entries.take(2).forEach { parts.add("${it.key}=${it.value}") }
        return parts.joinToString("  ·  ")
    }

    private fun fmtTime(ms: Long): String {
        val sec = (ms / 1000) % 86400
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    }
}
