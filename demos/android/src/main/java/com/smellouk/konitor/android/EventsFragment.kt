package com.smellouk.konitor.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.smellouk.konitor.Konitor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsFragment : Fragment() {

    private data class EventRow(
        val name: String,
        val wallClockMs: Long,
        val durationMs: Long?
    )

    private val events: MutableList<EventRow> = mutableListOf()
    private var adapter: EventAdapter? = null
    private var recyclerView: RecyclerView? = null
    private var emptyText: TextView? = null
    private var btnVideo: MaterialButton? = null

    @Volatile
    private var isVideoRecording: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_events, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.eventsList)
        emptyText = view.findViewById(R.id.emptyText)
        btnVideo = view.findViewById(R.id.btnPresetVideoPlayback)
        val btnLog = view.findViewById<MaterialButton>(R.id.btnLog)
        val input = view.findViewById<TextInputEditText>(R.id.customEventInput)

        adapter = EventAdapter(events)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        recyclerView?.adapter = adapter

        view.findViewById<MaterialButton>(R.id.btnPresetUserLogin).setOnClickListener {
            logInstantPreset("user_login")
        }
        view.findViewById<MaterialButton>(R.id.btnPresetPurchase).setOnClickListener {
            logInstantPreset("purchase")
        }
        view.findViewById<MaterialButton>(R.id.btnPresetScreenView).setOnClickListener {
            logInstantPreset("screen_view")
        }
        btnVideo?.setOnClickListener { triggerVideoPlayback() }

        btnLog.setOnClickListener {
            val raw = input.text?.toString().orEmpty().trim()
            if (raw.isNotEmpty()) {
                Konitor.logEvent(raw)
                addRow(EventRow(name = raw, wallClockMs = System.currentTimeMillis(), durationMs = null))
                input.text?.clear()
            }
        }
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val raw = input.text?.toString().orEmpty().trim()
                if (raw.isNotEmpty()) {
                    Konitor.logEvent(raw)
                    addRow(EventRow(name = raw, wallClockMs = System.currentTimeMillis(), durationMs = null))
                    input.text?.clear()
                }
                true
            } else {
                false
            }
        }

        updateEmptyState()
    }

    override fun onDestroyView() {
        recyclerView = null
        emptyText = null
        btnVideo = null
        adapter = null
        super.onDestroyView()
    }

    private fun logInstantPreset(name: String) {
        Konitor.logEvent(name)
        addRow(EventRow(name = name, wallClockMs = System.currentTimeMillis(), durationMs = null))
    }

    private fun triggerVideoPlayback() {
        if (isVideoRecording) return
        isVideoRecording = true
        btnVideo?.text = "Recording..."
        btnVideo?.isEnabled = false
        val token = Konitor.startEvent("video_playback")
        val startWallMs = System.currentTimeMillis()
        Thread {
            try {
                Thread.sleep(VIDEO_DEMO_SLEEP_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            Konitor.endEvent(token)
            val durMs = System.currentTimeMillis() - startWallMs
            activity?.runOnUiThread {
                btnVideo?.text = "video_playback"
                btnVideo?.isEnabled = true
                isVideoRecording = false
                addRow(EventRow(name = "video_playback", wallClockMs = startWallMs, durationMs = durMs))
            }
        }.apply { isDaemon = true }.start()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addRow(row: EventRow) {
        events.add(0, row)
        while (events.size > MAX_ROWS) events.removeAt(events.size - 1)
        adapter?.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        emptyText?.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
    }

    private class EventAdapter(private val rows: List<EventRow>) :
        RecyclerView.Adapter<EventAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val typeBar: View = view.findViewById(R.id.eventTypeBar)
            val name: TextView = view.findViewById(R.id.eventName)
            val timestamp: TextView = view.findViewById(R.id.eventTimestamp)
            val duration: TextView = view.findViewById(R.id.eventDuration)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = rows[position]
            holder.name.text = row.name
            holder.timestamp.text = TS_FMT.format(Date(row.wallClockMs))
            if (row.durationMs != null) {
                holder.duration.visibility = View.VISIBLE
                holder.duration.text = "(${row.durationMs}ms)"
                holder.typeBar.setBackgroundResource(R.color.color_blue)
            } else {
                holder.duration.visibility = View.GONE
                holder.typeBar.setBackgroundResource(R.color.color_green)
            }
        }

        override fun getItemCount(): Int = rows.size

        companion object {
            private val TS_FMT = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        }
    }

    companion object {
        private const val MAX_ROWS: Int = 200
        private const val VIDEO_DEMO_SLEEP_MS: Long = 2_000L
    }
}
