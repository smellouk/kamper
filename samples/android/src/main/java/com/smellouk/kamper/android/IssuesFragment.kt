package com.smellouk.kamper.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.smellouk.kamper.issues.Issue
import com.smellouk.kamper.issues.IssueSpans
import com.smellouk.kamper.issues.IssueType
import com.smellouk.kamper.issues.Severity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class IssuesFragment : Fragment() {

    private val issues = mutableListOf<Issue>()
    private var adapter: IssueAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var emptyText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_issues, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.issuesList)
        emptyText = view.findViewById(R.id.emptyText)

        adapter = IssueAdapter(issues)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = adapter

        view.findViewById<View>(R.id.btnSlowSpan).setOnClickListener { triggerSlowSpan() }
        view.findViewById<View>(R.id.btnBlockUi).setOnClickListener { triggerAnr() }
        view.findViewById<View>(R.id.btnCrash).setOnClickListener { triggerCrash() }
        view.findViewById<View>(R.id.btnMemory).setOnClickListener { triggerMemoryPressure() }
        view.findViewById<View>(R.id.btnStrictMode).setOnClickListener { triggerStrictIo() }
        view.findViewById<View>(R.id.btnClearIssues).setOnClickListener { clearIssues() }

        updateEmptyState()
    }

    override fun onDestroyView() {
        recyclerView = null
        emptyText = null
        adapter = null
        super.onDestroyView()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addIssue(issue: Issue) {
        val act = activity ?: return
        act.runOnUiThread {
            issues.add(0, issue)
            if (issues.size > 100) issues.removeAt(issues.size - 1)
            adapter?.notifyDataSetChanged()
            updateEmptyState()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearIssues() {
        issues.clear()
        adapter?.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val empty = issues.isEmpty()
        recyclerView?.visibility = if (empty) View.GONE else View.VISIBLE
        emptyText?.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun triggerSlowSpan() {
        Executors.newSingleThreadExecutor().submit {
            IssueSpans.measure("demo-background-op", thresholdMs = 500L) {
                Thread.sleep(1500)
            }
        }
    }

    private fun triggerAnr() {
        Handler(Looper.getMainLooper()).post {
            Thread.sleep(6_000L)
        }
    }

    private fun triggerCrash() {
        Thread {
            throw RuntimeException("Demo crash triggered from K|Android sample")
        }.also { it.isDaemon = true; it.start() }
    }

    private fun triggerMemoryPressure() {
        Executors.newSingleThreadExecutor().submit {
            val chunks = mutableListOf<ByteArray>()
            try {
                repeat(20) { chunks.add(ByteArray(5 * 1024 * 1024)) }
                Thread.sleep(3_000L)
            } finally {
                chunks.clear()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun triggerStrictIo() {
        val file = requireContext().filesDir.resolve("kamper_strict_test.txt")
        file.writeText("strict mode io test")
        file.delete()
    }
}

private class IssueAdapter(private val items: List<Issue>) :
    RecyclerView.Adapter<IssueAdapter.VH>() {

    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_issue, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val severityBar: View = view.findViewById(R.id.severityBar)
        private val typeLabel: TextView = view.findViewById(R.id.typeLabel)
        private val severityLabel: TextView = view.findViewById(R.id.severityLabel)
        private val timestampLabel: TextView = view.findViewById(R.id.timestampLabel)
        private val messageLabel: TextView = view.findViewById(R.id.messageLabel)
        private val detailsLabel: TextView = view.findViewById(R.id.detailsLabel)

        fun bind(issue: Issue) {
            val severityColor = severityColor(issue.severity)
            severityBar.setBackgroundColor(severityColor)
            typeLabel.text = typeShortName(issue.type)
            typeLabel.setBackgroundColor(typeColor(issue.type))
            severityLabel.text = issue.severity.name
            timestampLabel.text = timeFmt.format(Date(issue.timestampMs))
            messageLabel.text = issue.message

            val details = buildDetailsString(issue)
            if (details.isNotEmpty()) {
                detailsLabel.text = details
                detailsLabel.visibility = View.VISIBLE
            } else {
                detailsLabel.visibility = View.GONE
            }
        }

        private fun severityColor(s: Severity) = when (s) {
            Severity.CRITICAL -> 0xFFF38BA8.toInt()
            Severity.ERROR    -> 0xFFFAB387.toInt()
            Severity.WARNING  -> 0xFFF9E2AF.toInt()
            Severity.INFO     -> 0xFFA6E3A1.toInt()
        }

        private fun typeColor(t: IssueType) = when (t) {
            IssueType.ANR, IssueType.CRASH  -> 0xFFF38BA8.toInt()
            IssueType.SLOW_COLD_START,
            IssueType.SLOW_WARM_START,
            IssueType.SLOW_HOT_START        -> 0xFFFAB387.toInt()
            IssueType.DROPPED_FRAME         -> 0xFFF9E2AF.toInt()
            IssueType.SLOW_SPAN             -> 0xFF89B4FA.toInt()
            IssueType.MEMORY_PRESSURE,
            IssueType.NEAR_OOM              -> 0xFFCBA6F7.toInt()
            IssueType.STRICT_VIOLATION      -> 0xFF94E2D5.toInt()
        }

        private fun typeShortName(t: IssueType) = when (t) {
            IssueType.ANR               -> "ANR"
            IssueType.SLOW_COLD_START   -> "COLD START"
            IssueType.SLOW_WARM_START   -> "WARM START"
            IssueType.SLOW_HOT_START    -> "HOT START"
            IssueType.DROPPED_FRAME     -> "JANK"
            IssueType.SLOW_SPAN         -> "SPAN"
            IssueType.MEMORY_PRESSURE   -> "MEM"
            IssueType.NEAR_OOM          -> "OOM"
            IssueType.CRASH             -> "CRASH"
            IssueType.STRICT_VIOLATION  -> "STRICT"
        }

        private fun buildDetailsString(issue: Issue): String {
            val parts = mutableListOf<String>()
            issue.durationMs?.let { parts.add("${it}ms") }
            issue.threadName?.let { parts.add("thread=$it") }
            issue.details.entries.take(3).forEach { parts.add("${it.key}=${it.value}") }
            return parts.joinToString("  ·  ")
        }
    }
}
